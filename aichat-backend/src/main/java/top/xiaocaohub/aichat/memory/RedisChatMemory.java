package top.xiaocaohub.aichat.memory;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemory.class);
    private static final String PREFIX = "chat:mem:";

    // 固定配置
    private static final int MAX_CONTENT_LENGTH = 3000; // 每条消息最长3000字符
    private static final int SUMMARY_MAX_LENGTH = 500;  // 摘要最大长度
    private static final int TRUNCATE_FALLBACK_LENGTH = 200; // 降级截断长度
    private static final long TTL_HOURS = 24;

    // 可配置参数
    private final int maxMessages;
    private final int compressThreshold;
    private final int keepRecentMessages;

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final ChatClient chatClient;

    // 压缩专用线程池（避免占用主保存线程）
    private final ExecutorService compressExecutor;

    public RedisChatMemory(StringRedisTemplate redis, ObjectMapper mapper, @Lazy ChatClient chatClient) {
        this(redis, mapper, chatClient, 6, 4, 2);
    }

    public RedisChatMemory(StringRedisTemplate redis, ObjectMapper mapper, @Lazy ChatClient chatClient,
                           int maxMessages, int compressThreshold, int keepRecentMessages) {
        this.redis = redis;
        this.mapper = mapper;
        this.chatClient = chatClient;
        this.maxMessages = maxMessages;
        this.compressThreshold = compressThreshold;
        this.keepRecentMessages = keepRecentMessages;
        this.compressExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "chat-memory-compress");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    void trimExistingSessions() {
        try {
            Set<String> keys = redis.keys(PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.info("无聊天记忆需要修剪");
                return;
            }
            int count = 0;
            for (String key : keys) {
                Long size = redis.opsForList().size(key);
                if (size == null || size == 0) continue;
                log.info("修剪会话: {} ({}条消息)", key, size);
                // 直接清空重建，确保干净
                List<String> jsonList = redis.opsForList().range(key, 0, -1);
                redis.delete(key);
                if (jsonList == null) continue;
                // 只保留最后 maxMessages 条
                int start = Math.max(0, jsonList.size() - maxMessages);
                for (int i = start; i < jsonList.size(); i++) {
                    try {
                        MsgDto dto = mapper.readValue(jsonList.get(i), MsgDto.class);
                        String text = dto.content();
                        if (text.length() > MAX_CONTENT_LENGTH) {
                            text = text.substring(0, MAX_CONTENT_LENGTH) + "...[已截断]";
                            dto = new MsgDto(dto.role(), text);
                        }
                        redis.opsForList().rightPush(key, mapper.writeValueAsString(dto));
                    } catch (Exception ignored) {
                    }
                }
                count++;
            }
            log.info("启动修剪完成: 处理了 {} 个会话", count);
        } catch (Exception e) {
            log.warn("修剪聊天记忆失败: {}", e.getMessage());
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = PREFIX + conversationId;
        try {
            for (Message msg : messages) {
                MsgDto dto = MsgDto.from(msg);
                if (dto == null) continue;
                // 截断过长的消息
                String content = dto.content();
                if (content.length() > MAX_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_CONTENT_LENGTH) + "...[已截断]";
                    dto = new MsgDto(dto.role(), content);
                }
                redis.opsForList().rightPush(key, mapper.writeValueAsString(dto));
            }
            redis.opsForList().trim(key, -maxMessages, -1);
            redis.expire(key, java.time.Duration.ofHours(TTL_HOURS));

            // 检查是否需要压缩（异步执行，不阻塞主流程）
            Long size = redis.opsForList().size(key);
            if (size != null && size >= compressThreshold) {
                CompletableFuture.runAsync(() -> {
                    try {
                        compressContext(conversationId);
                    } catch (Exception e) {
                        log.warn("异步压缩上下文失败: {}", e.getMessage());
                    }
                }, compressExecutor);
            }

        } catch (Exception e) {
            log.warn("Redis 写入失败，降级跳过记忆存储: {}", e.getMessage());
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        try {
            List<String> jsonList = redis.opsForList().range(PREFIX + conversationId, 0, -1);
            List<Message> result = new ArrayList<>();
            if (jsonList == null) return result;
            for (String json : jsonList) {
                try {
                    MsgDto dto = mapper.readValue(json, MsgDto.class);
                    result.add(dto.toMessage());
                } catch (JsonProcessingException e) {
                    // 跳过损坏数据
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Redis 读取失败，降级为无记忆模式: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void clear(String conversationId) {
        try {
            redis.delete(PREFIX + conversationId);
        } catch (Exception e) {
            log.warn("Redis 删除失败: {}", e.getMessage());
        }
    }

    /**
     * 压缩上下文：将旧消息生成摘要，只保留最近的消息
     */
    private void compressContext(String conversationId) {
        String key = PREFIX + conversationId;
        try {
            List<String> jsonList = redis.opsForList().range(key, 0, -1);
            if (jsonList == null || jsonList.size() <= keepRecentMessages) {
                return; // 消息数量未达到压缩阈值
            }

            // 分离旧消息和新消息
            int splitIndex = jsonList.size() - keepRecentMessages;
            List<String> oldMessages = jsonList.subList(0, splitIndex);
            List<String> newMessages = jsonList.subList(splitIndex, jsonList.size());

            // 生成旧消息的摘要
            String summary = generateSummary(oldMessages);

            // 清空并重建：摘要 + 新消息
            redis.delete(key);

            // 添加摘要作为系统消息
            MsgDto summaryDto = new MsgDto("system", "历史对话摘要：" + summary);
            redis.opsForList().rightPush(key, mapper.writeValueAsString(summaryDto));

            // 添加新消息
            for (String msg : newMessages) {
                redis.opsForList().rightPush(key, msg);
            }

            // 重置TTL
            redis.expire(key, java.time.Duration.ofHours(TTL_HOURS));

            log.info("压缩会话上下文: {} ({}条旧消息 -> 摘要 + {}条新消息)",
                    conversationId, oldMessages.size(), newMessages.size());

        } catch (Exception e) {
            log.warn("压缩上下文失败: {}", e.getMessage());
        }
    }

    /**
     * 生成消息摘要（调用AI）
     */
    private String generateSummary(List<String> messages) {
        try {
            // 构建摘要请求
            StringBuilder context = new StringBuilder();
            for (String msg : messages) {
                MsgDto dto = mapper.readValue(msg, MsgDto.class);
                context.append(dto.role()).append(": ").append(dto.content()).append("\n");
            }

            // 如果内容不长，直接返回
            if (context.length() <= SUMMARY_MAX_LENGTH) {
                return context.toString();
            }

            // 调用AI生成摘要
            String prompt = "请将以下对话内容压缩成一段简洁的摘要，保留关键信息，不超过" + SUMMARY_MAX_LENGTH + "字。只返回摘要内容，不要加引号或其他符号：\n\n" + context;

            String summary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 清理和截断
            summary = summary.trim();
            if (summary.length() > SUMMARY_MAX_LENGTH) {
                summary = summary.substring(0, SUMMARY_MAX_LENGTH) + "...";
            }

            return summary;

        } catch (Exception e) {
            log.warn("AI生成摘要失败，使用降级策略: {}", e.getMessage());
            // 降级策略：截断旧消息
            return fallbackSummary(messages);
        }
    }

    /**
     * 降级策略：截断旧消息生成摘要
     */
    private String fallbackSummary(List<String> messages) {
        StringBuilder fallback = new StringBuilder();
        for (String msg : messages) {
            try {
                MsgDto dto = mapper.readValue(msg, MsgDto.class);
                fallback.append(dto.role()).append(": ");
                String content = dto.content();
                if (content.length() > TRUNCATE_FALLBACK_LENGTH) {
                    content = content.substring(0, TRUNCATE_FALLBACK_LENGTH) + "...";
                }
                fallback.append(content).append("; ");
            } catch (Exception ignored) {
            }
        }

        String result = fallback.toString();
        if (result.length() > SUMMARY_MAX_LENGTH) {
            result = result.substring(0, SUMMARY_MAX_LENGTH) + "...";
        }
        return result;
    }

    record MsgDto(String role, String content) {
        static MsgDto from(Message msg) {
            String role;
            if (msg instanceof UserMessage) role = "user";
            else if (msg instanceof AssistantMessage) role = "assistant";
            else if (msg instanceof SystemMessage) role = "system";
            else return null;
            String text = msg.getText() != null ? msg.getText() : "";
            return new MsgDto(role, text);
        }

        Message toMessage() {
            return switch (role) {
                case "user" -> new UserMessage(content);
                case "assistant" -> new AssistantMessage(content);
                case "system" -> new SystemMessage(content);
                default -> new UserMessage(content);
            };
        }
    }
}
