package top.xiaocaohub.aichat.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.xiaocaohub.aichat.dto.AuthResponse;
import top.xiaocaohub.aichat.dto.EmailCodeRequest;
import top.xiaocaohub.aichat.service.EmailService;

@Slf4j
@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send-code")
    public AuthResponse sendCode(@RequestBody EmailCodeRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            return new AuthResponse(400, "邮箱不能为空");
        }
        try {
            emailService.sendCode(req.getEmail());
            return new AuthResponse(200, "验证码已发送，请查收邮箱");
        } catch (RuntimeException e) {
            log.warn("发送验证码受限：{}", e.getMessage());
            return new AuthResponse(429, e.getMessage());
        } catch (Exception e) {
            log.error("发送验证码失败", e);
            return new AuthResponse(500, "发送失败，请稍后重试");
        }
    }
}
