package top.xiaocaohub.aichat.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class InputSanitizer {

    // XSS攻击模式（仅检测恶意脚本，不阻止正常编程讨论）
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script[^>]*>|</script>|javascript:|on\\w+\\s*=|<iframe|<object|<embed|<form",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 清理用户输入，防止XSS攻击
     * 注意：不转义 HTML 实体，因为聊天内容可能包含代码示例
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.trim();
    }

    /**
     * 检查是否包含XSS攻击代码
     */
    public boolean containsXss(String input) {
        if (input == null) {
            return false;
        }
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * 验证输入安全性
     * 注意：不检查 SQL 注入，因为 JPA 使用参数化查询，不存在 SQL 注入风险
     */
    public ValidationResult validate(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ValidationResult(false, "输入不能为空");
        }

        if (input.length() > 10000) {
            return new ValidationResult(false, "输入长度超出限制");
        }

        if (containsXss(input)) {
            return new ValidationResult(false, "输入包含不安全内容");
        }

        return new ValidationResult(true, null);
    }

    /**
     * 验证结果
     */
    public record ValidationResult(boolean valid, String message) {}
}
