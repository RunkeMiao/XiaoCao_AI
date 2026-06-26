package top.xiaocaohub.aichat.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import top.xiaocaohub.aichat.dto.ChatMessageResponse;
import top.xiaocaohub.aichat.dto.ChatRequest;
import top.xiaocaohub.aichat.dto.ChatSessionResponse;
import top.xiaocaohub.aichat.dto.SaveMessageRequest;
import top.xiaocaohub.aichat.service.ChatService;

import java.util.ArrayList;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final StringRedisTemplate redis;
    private final ChatService chatService;

    public ChatController(ChatClient chatClient, ChatMemory chatMemory,
                         StringRedisTemplate redis, ChatService chatService) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.redis = redis;
        this.chatService = chatService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request,
                            @AuthenticationPrincipal Long userId) {
        // 验证会话属于当前用户
        try {
            chatService.getSession(request.sessionId(), userId);
        } catch (RuntimeException e) {
            return Flux.error(new RuntimeException("会话不存在或无权访问"));
        }

        // 保存用户消息到数据库
        chatService.saveMessage(request.sessionId(), "user", request.message());

        // 使用用户ID隔离Redis中的聊天记忆
        String conversationId = userId + ":" + request.sessionId();
        log.info("聊天请求 - userId: {}, sessionId: {}, conversationId: {}", userId, request.sessionId(), conversationId);

        // 为当前请求创建advisor，设置conversationId
        Advisor advisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        return chatClient.prompt()
                .system("""
                        你是一个 AI 助手。请严格遵守以下 Markdown 输出规范：
                        1. 标题（# ## ### 等）必须独占一行，标题前后必须有一个空行。
                        2. 普通段落之间用一个空行分隔。
                        3. 列表项之间用一个空行分隔，使排版清晰。
                        4. 表格直接用 Markdown 表格语法，不要用代码块包裹。
                        5. 只有代码示例才使用 ``` 代码块，并标注语言。
                        6. 行内代码、加粗、链接等使用标准 Markdown 语法。
                        7. 保持回答简洁、准确、结构清晰。

                        【重要规则】
                        - 当用户询问当前时间、日期时，你必须调用 getCurrentDateTime 工具，不要凭记忆回答。
                        - 当用户询问任何股票、股价、行情、K线、涨跌停、公司信息、股东、分红、指数相关内容时，你必须调用对应的股票工具，绝对不要凭记忆编造数据。
                        - 你只能查询A股、北交所、科创板的数据，不支持港股和美股。如果用户问港股或美股，请告知暂不支持。

                        【可用的股票工具及使用方法】
                        1. getStockQuote(stockCode) - 查实时行情，stockCode传纯数字代码如"000001"、"600519"
                        2. getStockKline(stockCodeMarket, period, limit) - 查K线，stockCodeMarket传"000001.SZ"或"600519.SH"，period传"d"或"w"或"m"，limit传数字如10
                        3. getStockHistoryKline(stockCodeMarket, period, startDate, endDate, limit) - 查历史K线
                        4. getZtStocks(date) - 查涨停股池，date传"2025-01-15"格式
                        5. getDtStocks(date) - 查跌停股池
                        6. getQsStocks(date) - 查强势股池
                        7. getNewStocks() - 查新股列表
                        8. getCompanyInfo(stockCode) - 查公司简介
                        9. getDividendHistory(stockCode) - 查历年分红
                        10. getTopShareholders(stockCode) - 查十大股东
                        11. getIndexQuote(indexCode) - 查指数行情，indexCode传"000001.SH"或"399001.SZ"
                        """)
                .user(request.message())
                .advisors(advisor)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .stream()
                .content()
                .doOnComplete(() -> log.info("SSE流完成"))
                .doOnError(e -> log.warn("SSE流错误: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.warn("SSE 流结束异常（数据已发送）: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    // ===== 会话管理接口 =====

    @PostMapping("/sessions")
    public ChatSessionResponse createSession(@AuthenticationPrincipal Long userId) {
        return chatService.createSession(userId);
    }

    @GetMapping("/sessions")
    public List<ChatSessionResponse> getUserSessions(@AuthenticationPrincipal Long userId) {
        return chatService.getUserSessions(userId);
    }

    @GetMapping("/sessions/{sessionId}")
    public ChatSessionResponse getSession(@PathVariable String sessionId,
                                         @AuthenticationPrincipal Long userId) {
        return chatService.getSession(sessionId, userId);
    }

    @PutMapping("/sessions/{sessionId}")
    public void updateSessionTitle(@PathVariable String sessionId,
                                  @RequestParam String title,
                                  @AuthenticationPrincipal Long userId) {
        chatService.updateSessionTitleAsEdited(sessionId, userId, title);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void deleteSession(@PathVariable String sessionId,
                             @AuthenticationPrincipal Long userId) {
        chatService.deleteSession(sessionId, userId);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageResponse> getSessionMessages(@PathVariable String sessionId,
                                                       @AuthenticationPrincipal Long userId) {
        // 重建Redis上下文
        rebuildRedisContext(sessionId, userId);
        return chatService.getSessionMessages(sessionId, userId);
    }

    /**
     * 从数据库重建Redis上下文
     */
    private void rebuildRedisContext(String sessionId, Long userId) {
        String conversationId = userId + ":" + sessionId;
        log.info("尝试重建Redis上下文: {}", conversationId);
        try {
            // 检查Redis中是否已有上下文
            List<Message> existing = chatMemory.get(conversationId);
            log.info("Redis中现有消息数: {}", existing.size());
            if (!existing.isEmpty()) {
                log.info("Redis中已有上下文，跳过重建");
                return; // 已有上下文，不需要重建
            }

            // 从数据库加载最近的消息
            List<ChatMessageResponse> messages = chatService.getSessionMessagesInternal(sessionId);
            log.info("数据库中消息数: {}", messages.size());
            if (messages.isEmpty()) {
                log.info("数据库中没有消息，跳过重建");
                return;
            }

            // 只加载最近6条消息
            int start = Math.max(0, messages.size() - 6);
            List<Message> recentMessages = new ArrayList<>();
            for (int i = start; i < messages.size(); i++) {
                ChatMessageResponse msg = messages.get(i);
                switch (msg.getRole()) {
                    case "user" -> recentMessages.add(new UserMessage(msg.getContent()));
                    case "assistant" -> recentMessages.add(new AssistantMessage(msg.getContent()));
                }
            }

            log.info("准备写入Redis的消息数: {}", recentMessages.size());
            // 写入Redis
            if (!recentMessages.isEmpty()) {
                chatMemory.add(conversationId, recentMessages);
                log.info("重建Redis上下文完成: {}, 加载{}条消息", conversationId, recentMessages.size());
            }
        } catch (Exception e) {
            log.error("重建Redis上下文失败: {}", e.getMessage(), e);
        }
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public void saveMessage(@PathVariable String sessionId,
                           @RequestBody SaveMessageRequest request,
                           @AuthenticationPrincipal Long userId) {
        // 验证会话属于当前用户
        chatService.getSession(sessionId, userId);
        chatService.saveMessage(sessionId, request.getRole(), request.getContent());
    }

    @PostMapping("/sessions/{sessionId}/generate-title")
    public Map<String, String> generateTitle(@PathVariable String sessionId,
                                            @RequestBody Map<String, String> request,
                                            @AuthenticationPrincipal Long userId) {
        // 验证会话权限
        chatService.getSession(sessionId, userId);

        // 检查标题是否已手动编辑
        if (chatService.isTitleEdited(sessionId, userId)) {
            ChatSessionResponse session = chatService.getSession(sessionId, userId);
            return Map.of("title", session.getTitle());
        }

        String context = request.getOrDefault("context", "");
        String title = generateTitleWithAI(context);

        // 更新会话标题
        chatService.updateSessionTitle(sessionId, userId, title);

        return Map.of("title", title);
    }

    /**
     * 使用AI生成标题
     */
    private String generateTitleWithAI(String context) {
        try {
            String prompt = "请根据以下对话内容生成一个简洁的标题（不超过10个汉字），准确概括对话主题。只返回标题，不要加引号或其他符号：\n\n" + context;

            String title = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 清理和截断标题
            title = title.trim();
            if (title.length() > 10) {
                title = title.substring(0, 10);
            }

            // 移除可能的引号
            title = title.replaceAll("[\"'「」『』]", "");

            return title.isEmpty() ? "新对话" : title;

        } catch (Exception e) {
            log.warn("AI生成标题失败，使用降级策略: {}", e.getMessage());
            // 降级策略：取第一句话的前10个字符
            return extractKeywords(context);
        }
    }

    /**
     * 降级策略：提取关键词作为标题
     */
    private String extractKeywords(String context) {
        String[] lines = context.split("\n");
        for (String line : lines) {
            if (line.startsWith("用户:")) {
                String content = line.substring(3).trim();
                if (content.length() > 10) {
                    return content.substring(0, 10);
                }
                return content;
            }
        }
        return "新对话";
    }

    // ===== 调试接口（保持向后兼容） =====

    @GetMapping("/debug/keys")
    public Map<String, Object> listKeys() {
        Set<String> keys = redis.keys("chat:mem:*");
        List<String> keyList = keys != null ? new ArrayList<>(keys) : List.of();
        return Map.of("count", keyList.size(), "keys", keyList);
    }

    @GetMapping("/debug/memory")
    public Map<String, Object> getMemory(@RequestParam String sessionId) {
        List<Message> messages = chatMemory.get(sessionId);
        List<Map<String, String>> list = messages.stream()
                .map(m -> Map.of(
                        "type", m.getClass().getSimpleName(),
                        "text", m.getText() != null ? m.getText() : ""
                ))
                .toList();
        return Map.of("sessionId", sessionId, "messageCount", list.size(), "messages", list);
    }

    @DeleteMapping("/debug/memory")
    public Map<String, Object> clearMemory(@RequestParam(required = false) String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            chatMemory.clear(sessionId);
            return Map.of("cleared", sessionId);
        }
        Set<String> keys = redis.keys("chat:mem:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
        return Map.of("cleared", "all", "count", keys != null ? keys.size() : 0);
    }
}
