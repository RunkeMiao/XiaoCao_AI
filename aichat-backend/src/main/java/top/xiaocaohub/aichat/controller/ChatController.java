package top.xiaocaohub.aichat.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import top.xiaocaohub.aichat.dto.ChatRequest;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final StringRedisTemplate redis;

    public ChatController(ChatClient chatClient, ChatMemory chatMemory, StringRedisTemplate redis) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.redis = redis;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
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
                .advisors(a -> a.param("chat_memory_conversation_id", request.sessionId()))
                .stream()
                .content();
    }

    // ===== 调试接口 =====

    /** 列出所有聊天记忆 key */
    @GetMapping("/debug/keys")
    public Map<String, Object> listKeys() {
        Set<String> keys = redis.keys("chat:mem:*");
        List<String> keyList = keys != null ? new ArrayList<>(keys) : List.of();
        return Map.of("count", keyList.size(), "keys", keyList);
    }

    /** 查看某个 sessionId 的记忆消息 */
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

    /** 查看某个 sessionId 的 Redis 原始数据 */
    @GetMapping("/debug/raw")
    public Map<String, Object> getRaw(@RequestParam String sessionId) {
        String key = "chat:mem:" + sessionId;
        List<String> raw = redis.opsForList().range(key, 0, -1);
        Long ttl = redis.getExpire(key);
        return Map.of("key", key, "ttl", ttl + "s", "count", raw != null ? raw.size() : 0, "data", raw != null ? raw : List.of());
    }
}
