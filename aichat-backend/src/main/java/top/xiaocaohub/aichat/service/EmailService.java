package top.xiaocaohub.aichat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    private final String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        RedisTemplate<String, String> redisTemplate,
                        @Value("${spring.mail.username}") String fromEmail) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
        this.fromEmail = fromEmail;
    }

    private static final String CODE_PREFIX = "email:code:";
    private static final String SEND_LIMIT_PREFIX = "email:limit:";
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int SEND_COOLDOWN_SECONDS = 60;

    public void sendCode(String email) {
        // 频率限制：同一邮箱 60 秒内只能发一次
        String limitKey = SEND_LIMIT_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            Long ttl = redisTemplate.getExpire(limitKey, TimeUnit.SECONDS);
            throw new RuntimeException("发送太频繁，请 " + (ttl != null ? ttl : SEND_COOLDOWN_SECONDS) + " 秒后再试");
        }

        String code = generateCode();
        String key = CODE_PREFIX + email;

        // 存 Redis，5分钟过期
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 发邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("XiaoCao AI - 邮箱验证码");
            message.setText("您的验证码是：" + code + "\n5分钟内有效，请勿泄露给他人。");
            mailSender.send(message);
            // 设置发送频率限制
            redisTemplate.opsForValue().set(limitKey, "1", SEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);
            log.info("验证码已发送至：{}", email);
        } catch (Exception e) {
            log.error("发送邮件失败：{}", email, e);
            throw e;
        }
    }

    private static final String VERIFY_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    public boolean verifyCode(String email, String code) {
        String key = CODE_PREFIX + email;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(VERIFY_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), code);
        return result != null && result == 1L;
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
