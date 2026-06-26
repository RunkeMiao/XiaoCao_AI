package top.xiaocaohub.aichat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步 ChatMemory 包装器
 * 保存操作在后台线程执行，不阻塞流式响应的完成
 */
@Slf4j
public class AsyncChatMemory implements ChatMemory {

    private final ChatMemory delegate;
    private final ExecutorService executor;

    public AsyncChatMemory(ChatMemory delegate) {
        this(delegate, 4);
    }

    public AsyncChatMemory(ChatMemory delegate, int poolSize) {
        this.delegate = delegate;
        this.executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "async-chat-memory");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 异步保存，不阻塞调用线程
        CompletableFuture.runAsync(() -> {
            try {
                delegate.add(conversationId, messages);
                log.debug("异步保存聊天记忆完成: {}", conversationId);
            } catch (Exception e) {
                log.warn("异步保存聊天记忆失败: {}", e.getMessage());
            }
        }, executor);
    }

    @Override
    public List<Message> get(String conversationId) {
        // 读取操作同步执行（需要立即返回结果）
        return delegate.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        // 清除操作同步执行
        delegate.clear(conversationId);
    }
}
