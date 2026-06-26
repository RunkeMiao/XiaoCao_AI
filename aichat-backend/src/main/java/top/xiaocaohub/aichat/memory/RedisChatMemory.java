package top.xiaocaohub.aichat.memory;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemory.class);
    private static final String PREFIX = "chat:mem:";
    private static final int MAX_MESSAGES = 10;
    private static final int MAX_CONTENT_LENGTH = 8000;
    private static final long TTL_HOURS = 24;

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisChatMemory(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
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
                // 只保留最后 MAX_MESSAGES 条
                int start = Math.max(0, jsonList.size() - MAX_MESSAGES);
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
                redis.opsForList().rightPush(key, mapper.writeValueAsString(dto));
            }
            redis.opsForList().trim(key, -MAX_MESSAGES, -1);
            redis.expire(key, TTL_HOURS, TimeUnit.HOURS);
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

    record MsgDto(String role, String content) {
        static MsgDto from(Message msg) {
            String role;
            if (msg instanceof UserMessage) role = "user";
            else if (msg instanceof AssistantMessage) role = "assistant";
            else if (msg instanceof SystemMessage) role = "system";
            else return null;
            String text = msg.getText() != null ? msg.getText() : "";
            if (text.length() > MAX_CONTENT_LENGTH) {
                text = text.substring(0, MAX_CONTENT_LENGTH) + "...[已截断]";
            }
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
