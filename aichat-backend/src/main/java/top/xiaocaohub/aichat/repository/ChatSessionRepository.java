package top.xiaocaohub.aichat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.xiaocaohub.aichat.entity.ChatSession;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ChatSession> findBySessionIdAndUserId(String sessionId, Long userId);

    Optional<ChatSession> findBySessionId(String sessionId);

    boolean existsBySessionIdAndUserId(String sessionId, Long userId);
}
