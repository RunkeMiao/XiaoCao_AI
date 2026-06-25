package top.xiaocaohub.aichat.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import top.xiaocaohub.aichat.stock.client.BiYingApiClient;

import java.util.List;

/**
 * 公司信息服务
 */
@Service
public class CompanyService {

    private final BiYingApiClient client;

    public CompanyService(BiYingApiClient client) {
        this.client = client;
    }

    /** 公司简介 */
    public Object getCompanyInfo(String code) {
        return client.get("/hscp/gsjj/" + code + "/" + client.getLicence(), null, Object.class);
    }

    /** 所属指数 */
    public List<Object> getBelongIndex(String code) {
        return client.getList("/hscp/sszs/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 历届高管 */
    public List<Object> getExecutives(String code) {
        return client.getList("/hscp/ljgg/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 历届董事 */
    public List<Object> getDirectors(String code) {
        return client.getList("/hscp/ljds/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 历届监事 */
    public List<Object> getSupervisors(String code) {
        return client.getList("/hscp/ljjj/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 历年分红 */
    public List<Object> getDividendHistory(String code) {
        return client.getList("/hscp/jnfh/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 历年增发 */
    public List<Object> getAddShareHistory(String code) {
        return client.getList("/hscp/jnzf/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 限售解禁 */
    public List<Object> getLockUpExpiry(String code) {
        return client.getList("/hscp/jjxs/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 季度利润 */
    public List<Object> getQuarterlyProfit(String code) {
        return client.getList("/hscp/jdlr/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 季度现金流 */
    public List<Object> getQuarterlyCashflow(String code) {
        return client.getList("/hscp/jdxj/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 业绩预告 */
    public List<Object> getPerformanceForecast(String code) {
        return client.getList("/hscp/yjyg/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 财务指标 */
    public List<Object> getFinancialIndicators(String code) {
        return client.getList("/hscp/cwzb/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 十大股东 */
    public List<Object> getTopShareholders(String code) {
        return client.getList("/hscp/sdgd/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 十大流通股东 */
    public List<Object> getTopFlowShareholders(String code) {
        return client.getList("/hscp/ltgd/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 股东户数变化 */
    public List<Object> getHolderCountChange(String code) {
        return client.getList("/hscp/gdbh/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 基金持仓 */
    public List<Object> getFundHolding(String code) {
        return client.getList("/hscp/jjcg/" + code + "/" + client.getLicence(), null,
                new TypeReference<List<Object>>() {});
    }

    /** 股票基本信息 */
    public Object getInstrument(String codeMarket) {
        return client.get("/hsstock/instrument/" + codeMarket + "/" + client.getLicence(), null, Object.class);
    }

    /** 资金流向 */
    public List<Object> getMoneyFlow(String code, String startDate, String endDate, int limit) {
        String path = "/hsstock/history/transaction/" + code + "/" + client.getLicence();
        return client.getList(path, java.util.Map.of("st", startDate, "et", endDate, "lt", String.valueOf(limit)),
                new TypeReference<List<Object>>() {});
    }
}
