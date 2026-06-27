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

import jakarta.servlet.http.HttpServletResponse;
import top.xiaocaohub.aichat.dto.ChatMessageResponse;
import top.xiaocaohub.aichat.dto.ChatRequest;
import top.xiaocaohub.aichat.dto.ChatSessionResponse;
import top.xiaocaohub.aichat.dto.PageResponse;
import top.xiaocaohub.aichat.dto.SaveMessageRequest;
import top.xiaocaohub.aichat.exception.BusinessException;
import top.xiaocaohub.aichat.service.ChatService;
import top.xiaocaohub.aichat.util.InputSanitizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final String SYSTEM_PROMPT = """
            # 角色
            你是一个专业、友好的 AI 助手，擅长清晰地解答股票问题和分析股票数据。
            你是由BUG制造机小组制作出来的，组长是苗润轲，小组成员有刘恒磊、王佳辉、李佳涛、王伟。

            # 输出规范（Markdown）
            - 标题独占一行，前后空行；段落间用空行分隔。
            - 列表项之间空行分隔，提升可读性。
            - 表格直接用 Markdown 语法，不要包裹在代码块中。
            - 仅代码示例使用 ``` 块并标注语言。
            - 回答简洁准确，重点突出，避免冗余。

            # 工具调用规则（必须遵守）
            1. **时间/日期**：用户询问当前时间时，必须调用 getCurrentDateTime，禁止凭记忆回答。
            2. **股票数据**：涉及股价、行情、K线、涨跌停、公司信息、股东、分红、指数等，必须调用对应工具，严禁编造数据。
            3. **数据范围**：仅支持 A股（沪深）、北交所、科创板。港股/美股请回复"暂不支持"。

            # 可用工具
            | 工具 | 用途 | 参数说明 |
            |------|------|----------|
            | getStockQuote(stockCode) | 实时行情 | stockCode: 纯数字，如 "000001" |
            | getStockKline(stockCodeMarket, period, limit) | K线数据 | stockCodeMarket: "000001.SZ"；period: d/w/m；limit: 数字 |
            | getStockHistoryKline(stockCodeMarket, period, startDate, endDate, limit) | 历史K线 | 同上 + startDate/endDate: "2025-01-15" |
            | getZtStocks(date) | 涨停股池 | date: "2025-01-15" |
            | getDtStocks(date) | 跌停股池 | 同上 |
            | getQsStocks(date) | 强势股池 | 同上 |
            | getCompanyInfo(stockCode) | 公司简介 | stockCode: 纯数字 |
            | getDividendHistory(stockCode) | 历年分红 | 同上 |
            | getTopShareholders(stockCode) | 十大股东 | 同上 |
            | getIndexQuote(indexCode) | 指数行情 | indexCode: "000001.SH" 或 "399001.SZ" |
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final StringRedisTemplate redis;
    private final ChatService chatService;
    private final InputSanitizer inputSanitizer;

    public ChatController(ChatClient chatClient, ChatMemory chatMemory,
                         StringRedisTemplate redis, ChatService chatService,
                         InputSanitizer inputSanitizer) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.redis = redis;
        this.chatService = chatService;
        this.inputSanitizer = inputSanitizer;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request,
                            @AuthenticationPrincipal Long userId,
                            HttpServletResponse response) {
        // 防止代理/浏览器缓冲SSE流，改善手机端流式体验
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        // 输入验证
        InputSanitizer.ValidationResult validation = inputSanitizer.validate(request.message());
        if (!validation.valid()) {
            return Flux.error(new BusinessException(validation.message()));
        }

        // 验证会话属于当前用户
        try {
            chatService.getSession(request.sessionId(), userId);
        } catch (RuntimeException e) {
            return Flux.error(new BusinessException("会话不存在或无权访问"));
        }

        // 清理并保存用户消息到数据库
        String sanitizedMessage = inputSanitizer.sanitize(request.message());
        chatService.saveMessage(request.sessionId(), "user", sanitizedMessage);

        // 使用用户ID隔离Redis中的聊天记忆
        String conversationId = userId + ":" + request.sessionId();
        String sessionId = request.sessionId();

        // 为当前请求创建advisor，设置conversationId
        Advisor advisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        // 使用线程安全的方式收集AI回复
        AtomicReference<StringBuilder> aiResponseRef = new AtomicReference<>(new StringBuilder());
        AtomicBoolean saved = new AtomicBoolean(false);

        // 保存AI回复的辅助方法
        Runnable saveAiResponse = () -> {
            if (saved.getAndSet(true)) {
                log.info("AI回复已保存过，跳过: sessionId={}", sessionId);
                return; // 避免重复保存
            }
            String aiReply = aiResponseRef.get().toString();
            log.info("准备保存AI回复, sessionId={}, 长度={}", sessionId, aiReply.length());
            if (!aiReply.isEmpty()) {
                try {
                    chatService.saveMessage(sessionId, "assistant", aiReply);
                    log.info("AI回复已保存到数据库: {}, 内容长度: {}", sessionId, aiReply.length());
                } catch (Exception e) {
                    log.error("保存AI回复失败: {}", e.getMessage(), e);
                    saved.set(false); // 保存失败，允许重试
                }
            } else {
                log.warn("AI回复为空，跳过保存: sessionId={}", sessionId);
                saved.set(false); // 回复为空，允许重试
            }
        };

        // 流式调用：失败自动重试1次，仍失败则降级为非流式
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(sanitizedMessage)
                .advisors(advisor)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .stream()
                .content()
                .retry(1)
                .doOnNext(chunk -> {
                    // 线程安全地追加
                    synchronized (aiResponseRef) {
                        aiResponseRef.get().append(chunk);
                    }
                    log.debug("收到chunk: {}", chunk);
                })  // 收集每个chunk
                .doOnComplete(() -> {
                    log.info("流式输出完成，触发保存: sessionId={}, 总长度={}", sessionId, aiResponseRef.get().length());
                    saveAiResponse.run();
                })  // 正常结束时保存
                .doOnCancel(() -> {
                    // 用户取消时保存已生成的部分回复
                    log.info("用户取消了流式生成，保存部分回复: sessionId={}, 当前长度={}", sessionId, aiResponseRef.get().length());
                    saveAiResponse.run();
                })
                .doOnError(e -> {
                    log.error("流式输出出错: sessionId={}, error={}", sessionId, e.getMessage(), e);
                })
                .doFinally(signal -> {
                    log.info("流式信号结束: sessionId={}, signal={}, 已收集长度={}, 已保存={}", sessionId, signal, aiResponseRef.get().length(), saved.get());
                    // 如果 doOnComplete/doOnCancel 没有触发保存，这里作为最后保障
                    if (!saved.get() && aiResponseRef.get().length() > 0) {
                        log.warn("doFinally 触发保障保存: sessionId={}", sessionId);
                        saveAiResponse.run();
                    }
                })
                .onErrorResume(e -> {
                    log.warn("流式调用失败(已重试)，降级为非流式: {}", e.getMessage());
                    try {
                        String fallback = chatClient.prompt()
                                .system(SYSTEM_PROMPT)
                                .user(sanitizedMessage)
                                .advisors(advisor)
                                .advisors(a2 -> a2.param("chat_memory_conversation_id", conversationId))
                                .call()
                                .content();
                        // 非流式降级也要保存到数据库
                        if (fallback != null && !fallback.startsWith("⚠️")) {
                            chatService.saveMessage(sessionId, "assistant", fallback);
                        }
                        return Flux.just(fallback != null ? fallback : "⚠️ 服务暂时不可用，请稍后重试。");
                    } catch (Exception ex) {
                        log.error("非流式降级也失败: {}", ex.getMessage());
                        return Flux.just("⚠️ 服务暂时不可用，请稍后重试。");
                    }
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

    @GetMapping("/sessions/{sessionId}/messages/paged")
    public PageResponse<ChatMessageResponse> getSessionMessagesPaged(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Long userId) {
        return chatService.getSessionMessagesPaged(sessionId, userId, page, size);
    }

    /**
     * 从数据库重建Redis上下文
     */
    private void rebuildRedisContext(String sessionId, Long userId) {
        String conversationId = userId + ":" + sessionId;
        try {
            // 检查Redis中是否已有上下文
            List<Message> existing = chatMemory.get(conversationId);
            if (!existing.isEmpty()) {
                return; // 已有上下文，不需要重建
            }

            // 从数据库加载最近的消息
            List<ChatMessageResponse> messages = chatService.getSessionMessagesInternal(sessionId);
            if (messages.isEmpty()) {
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

            // 写入Redis
            if (!recentMessages.isEmpty()) {
                chatMemory.add(conversationId, recentMessages);
                log.info("重建Redis上下文: {}, 加载{}条消息", conversationId, recentMessages.size());
            }
        } catch (Exception e) {
            log.warn("重建Redis上下文失败: {}", e.getMessage());
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
