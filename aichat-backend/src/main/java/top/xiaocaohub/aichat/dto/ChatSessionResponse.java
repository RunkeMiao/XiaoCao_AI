package top.xiaocaohub.aichat.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionResponse {
    private String sessionId;
    private String title;
    private Boolean titleEdited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
