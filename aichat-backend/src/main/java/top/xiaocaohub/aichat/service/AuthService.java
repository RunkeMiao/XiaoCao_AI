package top.xiaocaohub.aichat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import top.xiaocaohub.aichat.dto.AuthResponse;
import top.xiaocaohub.aichat.dto.LoginRequest;
import top.xiaocaohub.aichat.dto.RegisterRequest;
import top.xiaocaohub.aichat.dto.ResetPasswordRequest;
import top.xiaocaohub.aichat.entity.User;
import top.xiaocaohub.aichat.repository.UserRepository;
import top.xiaocaohub.aichat.util.JwtUtil;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String REGISTER_LIMIT_PREFIX = "register:limit:";
    private static final int MAX_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int REGISTER_COOLDOWN_SECONDS = 60;

    public AuthResponse register(RegisterRequest request) {
        // 频率限制：同一邮箱 60 秒内只能注册一次
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String regLimitKey = REGISTER_LIMIT_PREFIX + request.getEmail();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(regLimitKey))) {
                return new AuthResponse(429, "操作过于频繁，请稍后再试");
            }
        }

        // 参数校验
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return new AuthResponse(400, "用户名不能为空");
        }
        if (request.getUsername().length() < 6 || request.getUsername().length() > 20) {
            return new AuthResponse(400, "用户名长度需在 6-20 个字符之间");
        }
        if (!request.getUsername().matches("^[a-zA-Z0-9_]+$")) {
            return new AuthResponse(400, "用户名只能包含字母、数字和下划线");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return new AuthResponse(400, "邮箱不能为空");
        }
        if (!request.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return new AuthResponse(400, "邮箱格式不正确");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            return new AuthResponse(400, "验证码不能为空");
        }
        String pwdError = validatePassword(request.getPassword());
        if (pwdError != null) {
            return new AuthResponse(400, pwdError);
        }

        // 校验用户名是否重复
        if (userRepository.existsByUsername(request.getUsername())) {
            return new AuthResponse(400, "用户名已被注册");
        }

        // 校验邮箱是否重复
        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(400, "邮箱已被注册");
        }

        // 校验验证码
        if (!emailService.verifyCode(request.getEmail(), request.getCode())) {
            return new AuthResponse(400, "验证码错误或已过期");
        }

        // 创建用户（捕获唯一约束冲突）
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .realName(request.getRealName())
                .build();
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            return new AuthResponse(400, "用户名或邮箱已被注册");
        }

        // 注册成功，设置频率限制
        redisTemplate.opsForValue().set(REGISTER_LIMIT_PREFIX + request.getEmail(), "1",
                REGISTER_COOLDOWN_SECONDS, TimeUnit.SECONDS);

        // 注册成功直接生成双 Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getTokenVersion());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getTokenVersion());
        return new AuthResponse(200, "注册成功", accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        // 参数校验
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return new AuthResponse(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return new AuthResponse(400, "密码不能为空");
        }

        // 检查是否被锁定
        String failKey = LOGIN_FAIL_PREFIX + request.getUsername();
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;

        if (failCount >= MAX_FAIL_COUNT) {
            Long ttl = redisTemplate.getExpire(failKey, TimeUnit.SECONDS);
            return new AuthResponse(429, "登录失败次数过多，请 " + (ttl / 60 + 1) + " 分钟后再试");
        }

        // 支持用户名或邮箱登录
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElse(null);

        if (user == null) {
            incrementFailCount(failKey);
            return new AuthResponse(401, "用户名或密码错误");
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            incrementFailCount(failKey);
            return new AuthResponse(401, "用户名或密码错误");
        }

        // 校验账号状态
        if (user.getStatus() == 0) {
            return new AuthResponse(403, "账号已被禁用");
        }

        // 登录成功，清除失败计数
        redisTemplate.delete(failKey);

        // 生成双 Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getTokenVersion());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getTokenVersion());
        return new AuthResponse(200, "登录成功", accessToken, refreshToken);
    }

    private void incrementFailCount(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次失败，设置过期时间
            redisTemplate.expire(key, LOCK_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * 校验密码强度，返回 null 表示通过，否则返回错误信息
     */
    private String validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return "密码长度不能少于6位";
        }
        if (password.length() > 18) {
            return "密码长度不能超过18位";
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            return "密码必须包含字母和数字";
        }
        return null;
    }

    public AuthResponse resetPassword(ResetPasswordRequest request) {
        // 参数校验
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return new AuthResponse(400, "邮箱不能为空");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            return new AuthResponse(400, "验证码不能为空");
        }
        String pwdError = validatePassword(request.getNewPassword());
        if (pwdError != null) {
            return new AuthResponse(400, pwdError);
        }

        // 校验验证码
        if (!emailService.verifyCode(request.getEmail(), request.getCode())) {
            return new AuthResponse(400, "验证码错误或已过期");
        }

        // 查找用户
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return new AuthResponse(404, "该邮箱未注册");
        }

        // 更新密码 + 递增 tokenVersion 使旧 Token 失效
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        return new AuthResponse(200, "密码重置成功");
    }
}
