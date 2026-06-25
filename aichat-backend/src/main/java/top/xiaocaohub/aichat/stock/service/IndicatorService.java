package top.xiaocaohub.aichat.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import top.xiaocaohub.aichat.stock.client.BiYingApiClient;

import java.util.List;
import java.util.Map;

/**
 * 技术指标服务（MACD/MA/BOLL/KDJ）
 */
@Service
public class IndicatorService {

    private final BiYingApiClient client;

    public IndicatorService(BiYingApiClient client) {
        this.client = client;
    }

    // ==================== 沪深股票指标 ====================

    /** 股票MACD */
    public List<Object> getStockMacd(String code, String period, String adjust,
                                      String startDate, String endDate, int limit) {
        String path = "/hsstock/history/macd/" + code + "/" + period + "/" + adjust + "/" + client.getLicence();
        return client.getList(path, params(startDate, endDate, limit), new TypeReference<List<Object>>() {});
    }

    /** 股票MA均线 */
    public List<Object> getStockMa(String code, String period, String adjust,
                                    String startDate, String endDate, int limit) {
        String path = "/hsstock/history/ma/" + code + "/" + period + "/" + adjust + "/" + client.getLicence();
        return client.getList(path, params(startDate, endDate, limit), new TypeReference<List<Object>>() {});
    }

    /** 股票布林带 */
    public List<Object> getStockBoll(String code, String period, String adjust,
                                      String startDate, String endDate, int limit) {
        String path = "/hsstock/history/boll/" + code + "/" + period + "/" + adjust + "/" + client.getLicence();
        return client.getList(path, params(startDate, endDate, limit), new TypeReference<List<Object>>() {});
    }

    /** 股票KDJ */
    public List<Object> getStockKdj(String code, String period, String adjust,
                                     String startDate, String endDate, int limit) {
        String path = "/hsstock/history/kdj/" + code + "/" + period + "/" + adjust + "/" + client.getLicence();
        return client.getList(path, params(startDate, endDate, limit), new TypeReference<List<Object>>() {});
    }

    // ==================== 指数指标 ====================

    /** 指数MACD */
    public List<Object> getIndexMacd(String code, String period,
                                      String startDate, String endDate, int limit) {
        String path = "/hsindex/history/macd/" + code + "/" + period + "/" + client.getLicence();
        return client.getList(path, params(startDate, endDate, limit), new TypeReference<List<Object>>() {});
    }

    /** 指数MA均线 */
    public List<Object> getIndexMa(String code, String period,
                                    String startDate, String endDate, int limit) {
        String path = "/hsindex/history/ma/" + code + "/" + period + "/" + client.getLicence();
        return client.getList(path, params(startDate, endDate, limit), new TypeReference<List<Object>>() {});
    }

    /** 指数布林带 */
    public List<Object> getIndexBoll(String code, String period,
                                      String startDate, String endDate, int limit) {
        String path = "/hsindex/history/boll/" + code + "/" + period + "/" + client.getLicence();
        return client.getList(path, params(startDate, endDate, limit), new TypeReference<List<Object>>() {});
    }

    /** 指数KDJ */
    public List<Object> getIndexKdj(String code, String period,
                                     String startDate, String endDate, int limit) {
        String path = "/hsindex/history/kdj/" + code + "/" + period + "/" + client.getLicence();
        return client.getList(path, params(startDate, endDate, limit), new TypeReference<List<Object>>() {});
    }

    /** 股票技术指标概览（量比、涨速、多日涨幅等） */
    public List<Object> getStockIndicators(String codeMarket, String startDate, String endDate) {
        String path = "/hsstock/indicators/" + codeMarket + "/" + client.getLicence();
        return client.getList(path, Map.of("st", startDate, "et", endDate),
                new TypeReference<List<Object>>() {});
    }

    private Map<String, String> params(String startDate, String endDate, int limit) {
        return Map.of("st", startDate, "et", endDate, "lt", String.valueOf(limit));
    }
}
