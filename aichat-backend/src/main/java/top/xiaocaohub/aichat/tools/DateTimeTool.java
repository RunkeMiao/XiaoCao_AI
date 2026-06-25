package top.xiaocaohub.aichat.tools;

import org.springframework.ai.tool.annotation.Tool;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeTool {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    @Tool(description = "获取当前的日期和时间，返回格式为 yyyy-MM-dd HH:mm:ss（中国时区）")
    public String getCurrentDateTime() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(CHINA_ZONE)
                .format(java.time.Instant.now());
    }
}
