package top.xiaocaohub.aichat.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String code;
    private String password;
    private String realName;
}
