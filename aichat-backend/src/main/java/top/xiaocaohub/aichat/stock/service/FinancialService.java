package top.xiaocaohub.aichat.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import top.xiaocaohub.aichat.stock.client.BiYingApiClient;

import java.util.List;
import java.util.Map;

/**
 * 财务数据服务（沪深+北交所通用）
 */
@Service
public class FinancialService {

    private final BiYingApiClient client;

    public FinancialService(BiYingApiClient client) {
        this.client = client;
    }

    // ==================== 沪深股票财务 ====================

    /** 沪深股票资产负债表 */
    public List<Object> getHsBalance(String code, String startDate, String endDate) {
        return getList("/hsstock/financial/balance/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 沪深股票利润表 */
    public List<Object> getHsIncome(String code, String startDate, String endDate) {
        return getList("/hsstock/financial/income/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 沪深股票现金流量表 */
    public List<Object> getHsCashflow(String code, String startDate, String endDate) {
        return getList("/hsstock/financial/cashflow/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 沪深股票每股指标 */
    public List<Object> getHsPerShareIndex(String code, String startDate, String endDate) {
        return getList("/hsstock/financial/pershareindex/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 沪深股票股本结构 */
    public List<Object> getHsCapital(String code, String startDate, String endDate) {
        return getList("/hsstock/financial/capital/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 沪深股票十大股东 */
    public List<Object> getHsTopHolder(String code, String startDate, String endDate) {
        return getList("/hsstock/financial/topholder/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 沪深股票十大流通股东 */
    public List<Object> getHsFlowHolder(String code, String startDate, String endDate) {
        return getList("/hsstock/financial/flowholder/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 沪深股票股东户数 */
    public List<Object> getHsHolderCount(String code, String startDate, String endDate) {
        return getList("/hsstock/financial/hm/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    // ==================== 北交所财务 ====================

    /** 北交所资产负债表 */
    public List<Object> getBjBalance(String code, String startDate, String endDate) {
        return getList("/bj/financial/balance/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 北交所利润表 */
    public List<Object> getBjIncome(String code, String startDate, String endDate) {
        return getList("/bj/financial/income/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 北交所现金流量表 */
    public List<Object> getBjCashflow(String code, String startDate, String endDate) {
        return getList("/bj/financial/cashflow/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 北交所每股指标 */
    public List<Object> getBjPerShareIndex(String code, String startDate, String endDate) {
        return getList("/bj/financial/pershareindex/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 北交所股本结构 */
    public List<Object> getBjCapital(String code, String startDate, String endDate) {
        return getList("/bj/financial/capital/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 北交所十大股东 */
    public List<Object> getBjTopHolder(String code, String startDate, String endDate) {
        return getList("/bj/financial/topholder/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 北交所十大流通股东 */
    public List<Object> getBjFlowHolder(String code, String startDate, String endDate) {
        return getList("/bj/financial/flowholder/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    /** 北交所股东户数 */
    public List<Object> getBjHolderCount(String code, String startDate, String endDate) {
        return getList("/bj/financial/hm/" + code + "/" + client.getLicence(), startDate, endDate);
    }

    private List<Object> getList(String path, String startDate, String endDate) {
        return client.getList(path, Map.of("st", startDate, "et", endDate),
                new TypeReference<List<Object>>() {});
    }
}
