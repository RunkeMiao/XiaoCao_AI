package top.xiaocaohub.aichat.stock.service;

import org.springframework.stereotype.Service;
import top.xiaocaohub.aichat.stock.client.BiYingApiClient;
import top.xiaocaohub.aichat.stock.dto.common.KlineItem;

import java.util.List;
import java.util.Map;

/**
 * K线数据服务
 */
@Service
public class KlineService {

    private final BiYingApiClient client;

    public KlineService(BiYingApiClient client) {
        this.client = client;
    }

    // ==================== 沪深股票 K线 ====================

    /** 沪深股票最新K线 */
    public List<KlineItem> getLatestKline(String codeMarket, String period, String adjust, int limit) {
        String path = "/hsstock/latest/" + codeMarket + "/" + period + "/" + adjust + "/" + client.getLicence();
        return client.getList(path, Map.of("lt", String.valueOf(limit)), KlineItem.class);
    }

    /** 沪深股票历史K线 */
    public List<KlineItem> getHistoryKline(String codeMarket, String period, String adjust,
                                            String startDate, String endDate, int limit) {
        String path = "/hsstock/history/" + codeMarket + "/" + period + "/" + adjust + "/" + client.getLicence();
        Map<String, String> params = Map.of(
                "st", startDate,
                "et", endDate,
                "lt", String.valueOf(limit)
        );
        return client.getList(path, params, KlineItem.class);
    }

    // ==================== 指数 K线 ====================

    /** 指数最新K线 */
    public List<KlineItem> getIndexLatestKline(String codeMarket, String period, int limit) {
        String path = "/hsindex/latest/" + codeMarket + "/" + period + "/" + client.getLicence();
        return client.getList(path, Map.of("lt", String.valueOf(limit)), KlineItem.class);
    }

    /** 指数历史K线 */
    public List<KlineItem> getIndexHistoryKline(String codeMarket, String period,
                                                 String startDate, String endDate) {
        String path = "/hsindex/history/" + codeMarket + "/" + period + "/" + client.getLicence();
        return client.getList(path, Map.of("st", startDate, "et", endDate), KlineItem.class);
    }

    // ==================== 北交所 K线 ====================

    /** 北交所历史K线 */
    public List<KlineItem> getBjHistoryKline(String codeMarket, String period, String adjust,
                                              String startDate, String endDate, int limit) {
        String path = "/bj/history/" + codeMarket + "/" + period + "/" + adjust + "/" + client.getLicence();
        Map<String, String> params = Map.of(
                "st", startDate,
                "et", endDate,
                "lt", String.valueOf(limit)
        );
        return client.getList(path, params, KlineItem.class);
    }
}
