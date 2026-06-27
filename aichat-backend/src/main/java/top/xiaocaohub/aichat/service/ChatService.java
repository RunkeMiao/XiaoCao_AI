package top.xiaocaohub.aichat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import top.xiaocaohub.aichat.dto.ChatMessageResponse;
import top.xiaocaohub.aichat.dto.ChatSessionResponse;
import top.xiaocaohub.aichat.dto.PageResponse;
import top.xiaocaohub.aichat.entity.ChatMessage;
import top.xiaocaohub.aichat.entity.ChatSession;
import top.xiaocaohub.aichat.exception.BusinessException;
import top.xiaocaohub.aichat.repository.ChatMessageRepository;
import top.xiaocaohub.aichat.repository.ChatSessionRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_MESSAGES_TO_LOAD = 50; // 最多加载50条消息

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemory chatMemory;
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建新会话
     */
    @Transactional
    public ChatSessionResponse createSession(Long userId) {
        String sessionId = "s_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .title("新对话")
                .build();

        chatSessionRepository.save(session);

        return ChatSessionResponse.builder()
                .sessionId(session.getSessionId())
                .title(session.getTitle())
                .titleEdited(session.getTitleEdited())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    /**
     * 获取用户的所有会话
     */
    public List<ChatSessionResponse> getUserSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(session -> ChatSessionResponse.builder()
                        .sessionId(session.getSessionId())
                        .title(session.getTitle())
                        .titleEdited(session.getTitleEdited())
                        .createdAt(session.getCreatedAt())
                        .updatedAt(session.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取会话详情（验证用户权限）
     */
    public ChatSessionResponse getSession(String sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> BusinessException.notFound("会话不存在或无权访问"));

        return ChatSessionResponse.builder()
                .sessionId(session.getSessionId())
                .title(session.getTitle())
                .titleEdited(session.getTitleEdited())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    /**
     * 更新会话标题
     */
    @Transactional
    public void updateSessionTitle(String sessionId, Long userId, String title) {
        ChatSession session = chatSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> BusinessException.notFound("会话不存在或无权访问"));

        session.setTitle(title);
        chatSessionRepository.save(session);
    }

    /**
     * 更新会话标题（标记为已编辑）
     */
    @Transactional
    public void updateSessionTitleAsEdited(String sessionId, Long userId, String title) {
        ChatSession session = chatSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> BusinessException.notFound("会话不存在或无权访问"));

        session.setTitle(title);
        session.setTitleEdited(true);
        chatSessionRepository.save(session);
    }

    /**
     * 检查会话标题是否已编辑
     */
    public boolean isTitleEdited(String sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> BusinessException.notFound("会话不存在或无权访问"));

        return session.getTitleEdited();
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> BusinessException.notFound("会话不存在或无权访问"));

        // 删除数据库
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);

        // 删除Redis中的上下文
        String conversationId = userId + ":" + sessionId;
        try {
            chatMemory.clear(conversationId);
            log.info("已清理Redis上下文: {}", conversationId);
        } catch (Exception e) {
            log.warn("清理Redis上下文失败: {}", e.getMessage());
        }
    }

    /**
     * 保存消息（使用编程式事务，兼容响应式流回调）
     */
    public void saveMessage(String sessionId, String role, String content) {
        transactionTemplate.executeWithoutResult(status -> {
            ChatMessage message = ChatMessage.builder()
                    .sessionId(sessionId)
                    .role(role)
                    .content(content)
                    .build();
            chatMessageRepository.save(message);
            log.debug("消息已保存: sessionId={}, role={}, length={}", sessionId, role, content.length());
        });
    }

    /**
     * 获取会话的所有消息（限制最大条数）
     */
    public List<ChatMessageResponse> getSessionMessages(String sessionId, Long userId) {
        // 验证用户有权访问该会话
        if (!chatSessionRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw BusinessException.notFound("会话不存在或无权访问");
        }

        return getSessionMessagesInternal(sessionId);
    }

    /**
     * 获取会话消息（不验证权限，用于内部调用，限制最大条数）
     */
    public List<ChatMessageResponse> getSessionMessagesInternal(String sessionId) {
        // 使用分页查询，只加载最近的消息（倒序获取后反转为正序）
        Pageable pageable = PageRequest.of(0, MAX_MESSAGES_TO_LOAD);
        List<ChatMessageResponse> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable)
                .stream()
                .map(message -> ChatMessageResponse.builder()
                        .id(message.getId())
                        .role(message.getRole())
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        // 反转为正序（ oldest first ）
        Collections.reverse(messages);
        return messages;
    }

    /**
     * 分页获取会话消息
     */
    public PageResponse<ChatMessageResponse> getSessionMessagesPaged(String sessionId, Long userId, int page, int size) {
        // 验证用户有权访问该会话
        if (!chatSessionRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw BusinessException.notFound("会话不存在或无权访问");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagePage = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, pageable);

        List<ChatMessageResponse> content = messagePage.getContent().stream()
                .map(message -> ChatMessageResponse.builder()
                        .id(message.getId())
                        .role(message.getRole())
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.of(content, page, size, messagePage.getTotalElements());
    }
}
