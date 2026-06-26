package top.xiaocaohub.aichat.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private int code;
    private String message;
    private String token;
    private String refreshToken;
    private String username;
    private String email;

    public AuthResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public AuthResponse(int code, String message, String token, String refreshToken) {
        this.code = code;
        this.message = message;
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public static AuthResponse me(String username, String email) {
        AuthResponse resp = new AuthResponse(200, "ok");
        resp.setUsername(username);
        resp.setEmail(email);
        return resp;
    }
}
