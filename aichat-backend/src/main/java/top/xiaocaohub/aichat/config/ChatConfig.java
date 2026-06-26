package top.xiaocaohub.aichat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.xiaocaohub.aichat.memory.RedisChatMemory;
import top.xiaocaohub.aichat.stock.tool.StockTool;
import top.xiaocaohub.aichat.tools.DateTimeTool;

import java.time.Duration;

@Configuration
public class ChatConfig {

    @Value("${app.chat-memory.pool-size:4}")
    private int chatMemoryPoolSize;

    @Value("${app.chat-memory.max-messages:6}")
    private int maxMessages;

    @Value("${app.chat-memory.compress-threshold:4}")
    private int compressThreshold;

    @Value("${app.chat-memory.keep-recent-messages:2}")
    private int keepRecentMessages;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * 异步 ChatMemory：保存操作在后台线程执行，不阻塞流式响应
     * 解决压缩上下文时同步调用 AI 导致的 5 秒延迟问题
     */
    @Bean
    public ChatMemory chatMemory(StringRedisTemplate redis, ObjectMapper mapper, ChatClient.Builder builder) {
        ChatClient tempClient = builder.build();
        RedisChatMemory redisChatMemory = new RedisChatMemory(redis, mapper, tempClient,
                maxMessages, compressThreshold, keepRecentMessages);
        return new AsyncChatMemory(redisChatMemory, chatMemoryPoolSize);
    }

    /**
     * 配置 AI API HTTP 客户端超时（OkHttp）
     * 通过 Spring AI 官方扩展点设置，解决长回复流超时问题
     */
    @Bean
    public OpenAiHttpClientBuilderCustomizer httpClientCustomizer() {
        return builder -> builder.timeout(
                Timeout.builder()
                        .connect(Duration.ofSeconds(30))
                        .read(Duration.ofSeconds(180))
                        .write(Duration.ofSeconds(60))
                        .build()
        );
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, StockTool stockTool) {
        return builder
                .defaultTools(new DateTimeTool(), stockTool)
                .build();
    }
}
