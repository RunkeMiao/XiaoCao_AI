package top.xiaocaohub.aichat.stock.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import top.xiaocaohub.aichat.stock.dto.common.QuoteItem;
import top.xiaocaohub.aichat.stock.service.*;

/**
 * 股票信息工具（注册给AI调用）
 */
@Component
public class StockTool {

    private static final Logger log = LoggerFactory.getLogger(StockTool.class);

    private final QuoteService quoteService;
    private final KlineService klineService;
    private final PoolService poolService;
    private final CompanyService companyService;
    private final ObjectMapper objectMapper;

    public StockTool(QuoteService quoteService, KlineService klineService,
                     PoolService poolService, CompanyService companyService,
                     ObjectMapper objectMapper) {
        this.quoteService = quoteService;
        this.klineService = klineService;
        this.poolService = poolService;
        this.companyService = companyService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "查询A股实时行情。传入股票代码（如000001），返回当前价格、涨跌幅、成交量等实时数据。")
    public String getStockQuote(
            @ToolParam(description = "股票代码，如000001、600519") String stockCode) {
        log.info("===== AI调用工具: getStockQuote, stockCode={} =====", stockCode);
        try {
            QuoteItem quote = quoteService.getHsQuote(stockCode);
            String result = toJson(quote);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询A股最新K线数据。传入股票代码和周期（d=日K、w=周K、m=月K、5=5分钟、15=15分钟、30=30分钟、60=60分钟），返回最新几条K线。")
    public String getStockKline(
            @ToolParam(description = "股票代码，如000001.SZ、600519.SH") String stockCodeMarket,
            @ToolParam(description = "K线周期: d=日K, w=周K, m=月K, 5=5分钟, 15=15分钟, 30=30分钟, 60=60分钟") String period,
            @ToolParam(description = "返回条数，如10、30") int limit) {
        log.info("===== AI调用工具: getStockKline, code={}, period={}, limit={} =====", stockCodeMarket, period, limit);
        try {
            var klines = klineService.getLatestKline(stockCodeMarket, period, "n", limit);
            String result = toJson(klines);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询A股历史K线数据。传入股票代码、周期、起止日期，返回指定时间段的K线数据。")
    public String getStockHistoryKline(
            @ToolParam(description = "股票代码，如000001.SZ、600519.SH") String stockCodeMarket,
            @ToolParam(description = "K线周期: d=日K, w=周K, m=月K") String period,
            @ToolParam(description = "开始日期，格式yyyyMMdd，如20240101") String startDate,
            @ToolParam(description = "结束日期，格式yyyyMMdd，如20250101") String endDate,
            @ToolParam(description = "返回条数，如100") int limit) {
        try {
            var klines = klineService.getHistoryKline(stockCodeMarket, period, "n", startDate, endDate, limit);
            return toJson(klines);
        } catch (Exception e) {
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询指定日期的涨停股池，返回涨停股票列表，包含涨停价格、连板数、封板时间等信息。")
    public String getZtStocks(
            @ToolParam(description = "日期，格式yyyy-MM-dd，如2025-01-15") String date) {
        log.info("===== AI调用工具: getZtStocks, date={} =====", date);
        try {
            var list = poolService.getZtgc(date);
            String result = toJson(list);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询指定日期的跌停股池，返回跌停股票列表。")
    public String getDtStocks(
            @ToolParam(description = "日期，格式yyyy-MM-dd，如2025-01-15") String date) {
        log.info("===== AI调用工具: getDtStocks, date={} =====", date);
        try {
            var list = poolService.getDtgc(date);
            String result = toJson(list);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询指定日期的强势股池，返回强势股票列表，包含涨幅、是否新高、连板统计等。")
    public String getQsStocks(
            @ToolParam(description = "日期，格式yyyy-MM-dd，如2025-01-15") String date) {
        log.info("===== AI调用工具: getQsStocks, date={} =====", date);
        try {
            var list = poolService.getQsgc(date);
            String result = toJson(list);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询新股列表，返回即将上市和最近上市的新股信息，包含申购日期、发行价格、上市日期等。")
    public String getNewStocks() {
        log.info("===== AI调用工具: getNewStocks =====");
        try {
            var list = poolService.getNewStockList();
            String result = toJson(list);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询公司基本信息，传入股票代码，返回公司名称、上市日期、经营范围、公司简介等。")
    public String getCompanyInfo(
            @ToolParam(description = "股票代码，如000001") String stockCode) {
        log.info("===== AI调用工具: getCompanyInfo, stockCode={} =====", stockCode);
        try {
            var info = companyService.getCompanyInfo(stockCode);
            String result = toJson(info);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询公司历年分红记录，传入股票代码，返回每10股送股、转增、派息等分红信息。")
    public String getDividendHistory(
            @ToolParam(description = "股票代码，如000001") String stockCode) {
        log.info("===== AI调用工具: getDividendHistory, stockCode={} =====", stockCode);
        try {
            var list = companyService.getDividendHistory(stockCode);
            String result = toJson(list);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询公司十大股东，传入股票代码，返回最新一期的十大股东名单及持股比例。")
    public String getTopShareholders(
            @ToolParam(description = "股票代码，如000001") String stockCode) {
        log.info("===== AI调用工具: getTopShareholders, stockCode={} =====", stockCode);
        try {
            var list = companyService.getTopShareholders(stockCode);
            String result = toJson(list);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询指数实时行情，传入指数代码（如000001.SH表示上证指数），返回最新价、涨跌幅等。")
    public String getIndexQuote(
            @ToolParam(description = "指数代码，如000001.SH（上证指数）、399001.SZ（深证成指）") String indexCode) {
        log.info("===== AI调用工具: getIndexQuote, indexCode={} =====", indexCode);
        try {
            QuoteItem quote = quoteService.getIndexQuote(indexCode);
            String result = toJson(quote);
            log.info("查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return "查询失败: " + e.getMessage();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
