package top.xiaocaohub.aichat.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import top.xiaocaohub.aichat.stock.client.BiYingApiClient;

import java.util.List;
import java.util.Map;

/**
 * 股池服务（涨跌停、强势股、次新股、炸板股）
 */
@Service
public class PoolService {

    private final BiYingApiClient client;

    public PoolService(BiYingApiClient client) {
        this.client = client;
    }

    /** 涨停股池 */
    public List<Object> getZtgc(String date) {
        return client.getList("/hslt/ztgc/" + date + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 跌停股池 */
    public List<Object> getDtgc(String date) {
        return client.getList("/hslt/dtgc/" + date + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 强势股池 */
    public List<Object> getQsgc(String date) {
        return client.getList("/hslt/qsgc/" + date + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 次新股池 */
    public List<Object> getCxgc(String date) {
        return client.getList("/hslt/cxgc/" + date + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 炸板股池 */
    public List<Object> getZbgc(String date) {
        return client.getList("/hslt/zbgc/" + date + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 涨跌停历史 */
    public List<Object> getStopPriceHistory(String codeMarket, String startDate, String endDate) {
        String path = "/hsstock/stopprice/history/" + codeMarket + "/" + client.getLicence();
        return client.getList(path, Map.of("st", startDate, "et", endDate),
                new TypeReference<List<Object>>() {});
    }

    /** 新股列表 */
    public List<Object> getNewStockList() {
        return client.getList("/hslt/new/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }
}
