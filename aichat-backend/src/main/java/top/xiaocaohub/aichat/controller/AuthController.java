package top.xiaocaohub.aichat.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import top.xiaocaohub.aichat.dto.AuthResponse;
import top.xiaocaohub.aichat.dto.LoginRequest;
import top.xiaocaohub.aichat.dto.RegisterRequest;
import top.xiaocaohub.aichat.dto.ResetPasswordRequest;
import top.xiaocaohub.aichat.service.AuthService;
import top.xiaocaohub.aichat.repository.UserRepository;
import top.xiaocaohub.aichat.util.JwtUtil;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse result = authService.register(request);
        setTokenCookies(result, response, 3600, 7 * 24 * 3600);
        return result;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse result = authService.login(request);
        if (result.getCode() == 200) {
            boolean rememberMe = request.isRememberMe();
            int accessMaxAge = rememberMe ? 7 * 24 * 3600 : 3600;
            int refreshMaxAge = rememberMe ? 30 * 24 * 3600 : 7 * 24 * 3600;
            setTokenCookies(result, response, accessMaxAge, refreshMaxAge);
        }
        return result;
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        // 从 Cookie 读取 refresh token
        String refreshToken = extractCookie(request, "refresh_token");
        if (refreshToken == null) {
            return new AuthResponse(401, "无刷新凭证");
        }

        try {
            var claims = jwtUtil.parseRefreshToken(refreshToken);
            Long userId = Long.parseLong(claims.getSubject());
            int tokenVersion = claims.get("tv", Integer.class);

            // 校验 tokenVersion
            return userRepository.findById(userId)
                    .filter(user -> user.getTokenVersion() == tokenVersion)
                    .map(user -> {
                        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getTokenVersion());
                        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getTokenVersion());

                        // 设置新的 access_token cookie
                        addCookie(response, "token", newAccessToken, 3600);
                        // 设置新的 refresh_token cookie
                        addCookie(response, "refresh_token", newRefreshToken, 7 * 24 * 3600);

                        return new AuthResponse(200, "刷新成功");
                    })
                    .orElse(new AuthResponse(401, "凭证无效"));
        } catch (Exception e) {
            return new AuthResponse(401, "凭证已过期");
        }
    }

    private void setTokenCookies(AuthResponse result, HttpServletResponse response,
                                  int accessMaxAge, int refreshMaxAge) {
        if (result.getCode() == 200 && result.getToken() != null) {
            addCookie(response, "token", result.getToken(), accessMaxAge);
            result.setToken(null);
        }
        if (result.getCode() == 200 && result.getRefreshToken() != null) {
            addCookie(response, "refresh_token", result.getRefreshToken(), refreshMaxAge);
            result.setRefreshToken(null);
        }
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @PostMapping("/reset-password")
    public AuthResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpServletResponse response) {
        addCookie(response, "token", "", 0);
        addCookie(response, "refresh_token", "", 0);
        return new AuthResponse(200, "已退出登录");
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return new AuthResponse(401, "未登录");
        }
        return userRepository.findById(userId)
                .map(user -> AuthResponse.me(user.getUsername(), user.getEmail(), user.getRealName()))
                .orElse(new AuthResponse(404, "用户不存在"));
    }
}
