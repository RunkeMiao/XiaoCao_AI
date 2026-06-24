package top.xiaocaohub.aichat.memory;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedisChatMemory implements ChatMemory {

    private static final String PREFIX = "chat:mem:";
    private static final int MAX_MESSAGES = 20;
    private static final long TTL_HOURS = 24;

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisChatMemory(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化消息失败", e);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
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
    }

    @Override
    public void clear(String conversationId) {
        redis.delete(PREFIX + conversationId);
    }

    record MsgDto(String role, String content) {
        static MsgDto from(Message msg) {
            String role;
            if (msg instanceof UserMessage) role = "user";
            else if (msg instanceof AssistantMessage) role = "assistant";
            else if (msg instanceof SystemMessage) role = "system";
            else return null;
            return new MsgDto(role, msg.getText() != null ? msg.getText() : "");
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
