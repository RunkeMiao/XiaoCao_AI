package top.xiaocaohub.aichat.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import top.xiaocaohub.aichat.stock.client.BiYingApiClient;
import top.xiaocaohub.aichat.stock.dto.common.StockItem;

import java.util.List;

/**
 * 股票/基金/指数 列表服务
 */
@Service
public class StockListService {

    private final BiYingApiClient client;

    public StockListService(BiYingApiClient client) {
        this.client = client;
    }

    /** 沪深股票列表 */
    public List<StockItem> getHsList() {
        return client.getList("/hslt/list/" + client.getLicence(), null, StockItem.class);
    }

    /** 北交所股票列表 */
    public List<StockItem> getBjList() {
        return client.getList("/bj/list/all/" + client.getLicence(), null, StockItem.class);
    }

    /** 北交所指数列表 */
    public List<StockItem> getBjIndexList() {
        return client.getList("/bj/list/index/" + client.getLicence(), null, StockItem.class);
    }

    /** 科创板股票列表 */
    public List<StockItem> getKcList() {
        return client.getList("/kc/list/all/" + client.getLicence(), null, StockItem.class);
    }

    /** 基金列表 */
    public List<StockItem> getFundList() {
        return client.getList("/fd/list/all/" + client.getLicence(), null, StockItem.class);
    }

    /** ETF列表 */
    public List<StockItem> getEtfList() {
        return client.getList("/fd/list/etf/" + client.getLicence(), null, StockItem.class);
    }

    /** 指数列表 */
    public List<StockItem> getIndexList() {
        return client.getList("/hsindex/list/" + client.getLicence(), null, StockItem.class);
    }

    /** 板块/概念列表 */
    public List<StockItem> getSectorsList() {
        return client.getList("/hslt/sectorslist/" + client.getLicence(), null, StockItem.class);
    }

    /** 一级市场列表 */
    public List<StockItem> getPrimaryList() {
        return client.getList("/hslt/primarylist/" + client.getLicence(), null, StockItem.class);
    }

    /** 板块详情 */
    public List<StockItem> getSectors(String sectorName) {
        return client.getList("/hslt/sectors/" + sectorName + "/" + client.getLicence(), null, StockItem.class);
    }

    /** 股票分类列表 */
    public List<Object> getZgList() {
        return client.getList("/hszg/list/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 指数/行业/概念成分股 */
    public List<StockItem> getZgGg(String code) {
        return client.getList("/hszg/gg/" + code + "/" + client.getLicence(), null, StockItem.class);
    }
}
