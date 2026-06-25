package top.xiaocaohub.aichat.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import top.xiaocaohub.aichat.stock.client.BiYingApiClient;
import top.xiaocaohub.aichat.stock.dto.common.QuoteItem;

import java.util.List;
import java.util.Map;

/**
 * 实时行情服务
 */
@Service
public class QuoteService {

    private final BiYingApiClient client;

    public QuoteService(BiYingApiClient client) {
        this.client = client;
    }

    /** 沪深股票实时交易 */
    public QuoteItem getHsQuote(String code) {
        return client.get("/hsrl/ssjy/" + code + "/" + client.getLicence(), null, QuoteItem.class);
    }

    /** 批量实时交易（最多20只） */
    public List<QuoteItem> getHsQuoteBatch(List<String> codes) {
        String joined = String.join(",", codes);
        return client.getList("/hsrl/ssjy_more/" + client.getLicence(),
                Map.of("stock_codes", joined), QuoteItem.class);
    }

    /** 股票实时行情（hsstock） */
    public QuoteItem getStockQuote(String code) {
        return client.get("/hsstock/real/time/" + code + "/" + client.getLicence(), null, QuoteItem.class);
    }

    /** 北交所股票实时行情 */
    public QuoteItem getBjQuote(String code) {
        return client.get("/bj/stock/real/time/" + code + "/" + client.getLicence(), null, QuoteItem.class);
    }

    /** 科创板实时行情 */
    public QuoteItem getKcQuote(String code) {
        return client.get("/kc/real/time/" + code + "/" + client.getLicence(), null, QuoteItem.class);
    }

    /** 基金实时行情 */
    public QuoteItem getFundQuote(String code) {
        return client.get("/fd/real/time/" + code + "/" + client.getLicence(), null, QuoteItem.class);
    }

    /** 指数实时行情 */
    public QuoteItem getIndexQuote(String code) {
        return client.get("/hsindex/real/time/" + code + "/" + client.getLicence(), null, QuoteItem.class);
    }

    /** 北交所指数实时行情 */
    public QuoteItem getBjIndexQuote(String code) {
        return client.get("/bj/index/real/time/" + code + "/" + client.getLicence(), null, QuoteItem.class);
    }

    /** 五档行情（通用） */
    public Object getFiveLevelQuote(String code, String market) {
        String path = switch (market) {
            case "bj" -> "/bj/stock/real/five/" + code + "/" + client.getLicence();
            case "kc" -> "/kc/real/five/" + code + "/" + client.getLicence();
            default -> "/hsstock/real/five/" + code + "/" + client.getLicence();
        };
        return client.get(path, null, Object.class);
    }

    /** 逐笔交易 */
    public List<Object> getTickData(String code) {
        return client.getList("/hsrl/zbjy/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 股票所属板块 */
    public List<Object> getStockSectors(String code) {
        return client.getList("/hszg/zg/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }
}
