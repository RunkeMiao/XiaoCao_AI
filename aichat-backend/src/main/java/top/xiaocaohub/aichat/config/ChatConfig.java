package top.xiaocaohub.aichat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.xiaocaohub.aichat.stock.tool.StockTool;
import top.xiaocaohub.aichat.tools.DateTimeTool;

import java.time.Duration;

@Configuration
public class ChatConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
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
                        .read(Duration.ofSeconds(120))
                        .write(Duration.ofSeconds(30))
                        .build()
        );
    }

    @Bean
    public Advisor chatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, Advisor chatMemoryAdvisor, StockTool stockTool) {
        return builder
                .defaultAdvisors(chatMemoryAdvisor)
                .defaultTools(new DateTimeTool(), stockTool)
                .build();
    }
}
