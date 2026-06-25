package top.xiaocaohub.aichat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.xiaocaohub.aichat.stock.tool.StockTool;
import top.xiaocaohub.aichat.tools.DateTimeTool;

@Configuration
public class ChatConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
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
