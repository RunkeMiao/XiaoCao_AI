package top.xiaocaohub.aichat.stock.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.xiaocaohub.aichat.stock.config.BiYingConfig;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 必盈API统一客户端
 */
@Component
public class BiYingApiClient {

    private static final Logger log = LoggerFactory.getLogger(BiYingApiClient.class);

    private final RestTemplate restTemplate;
    private final BiYingConfig config;
    private final ObjectMapper objectMapper;

    public BiYingApiClient(RestTemplate stockRestTemplate, BiYingConfig config, ObjectMapper objectMapper) {
        this.restTemplate = stockRestTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * GET 请求，返回原始 JSON 字符串
     */
    public String getRaw(String path, Map<String, String> params) {
        String url = config.getBaseUrl() + path;
        if (params != null && !params.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            params.forEach((k, v) -> joiner.add(k + "=" + v));
            url += "?" + joiner;
        }
        log.info("必盈API请求: {}", url);
        try {
            String json = restTemplate.getForObject(url, String.class);
            log.info("必盈API响应: {}", json != null && json.length() > 500 ? json.substring(0, 500) + "..." : json);
            return json;
        } catch (Exception e) {
            log.error("必盈API请求失败: {} - {}", url, e.getMessage());
            throw new RuntimeException("必盈API请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * GET 请求，反序列化为指定类型
     */
    public <T> T get(String path, Map<String, String> params, Class<T> type) {
        String json = getRaw(path, params);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("JSON反序列化失败: {}", e.getMessage());
            throw new RuntimeException("数据解析失败", e);
        }
    }

    /**
     * GET 请求，反序列化为 List
     */
    public <T> List<T> getList(String path, Map<String, String> params, Class<T> elementType) {
        String json = getRaw(path, params);
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            log.error("JSON反序列化失败: {}", e.getMessage());
            throw new RuntimeException("数据解析失败", e);
        }
    }

    /**
     * GET 请求，反序列化为 List（使用 TypeReference）
     */
    public <T> List<T> getList(String path, Map<String, String> params, TypeReference<List<T>> typeRef) {
        String json = getRaw(path, params);
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("JSON反序列化失败: {}", e.getMessage());
            throw new RuntimeException("数据解析失败", e);
        }
    }

    /**
     * 获取 licence
     */
    public String getLicence() {
        return config.getLicence();
    }

    /**
     * 构建带 licence 的路径
     */
    public String pathWithLicence(String template) {
        return template.replace("{licence}", config.getLicence());
    }
}
