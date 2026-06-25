package top.xiaocaohub.aichat.stock.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 实时行情数据（通用格式，适用于 hsrl/ssjy, hsstock/real/time 等）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuoteItem(
        @JsonProperty("dm") String code,       // 股票代码
        @JsonProperty("p") Double price,        // 当前价格
        @JsonProperty("o") Double open,         // 开盘价
        @JsonProperty("h") Double high,         // 最高价
        @JsonProperty("l") Double low,          // 最低价
        @JsonProperty("yc") Double preClose,    // 昨日收盘价
        @JsonProperty("pc") Double changePct,   // 涨跌幅(%)
        @JsonProperty("ud") Double changeAmt,   // 涨跌额
        @JsonProperty("zf") Double amplitude,   // 振幅(%)
        @JsonProperty("v") Long volume,         // 成交量(手)
        @JsonProperty("cje") Double turnover,   // 成交额(元)
        @JsonProperty("hs") Double turnoverRate,// 换手率(%)
        @JsonProperty("pe") Double pe,          // 市盈率
        @JsonProperty("sjl") Double pb,         // 市净率
        @JsonProperty("lt") Double circMarketCap,  // 流通市值
        @JsonProperty("sz") Double totalMarketCap, // 总市值
        @JsonProperty("lb") Double volumeRatio,    // 量比
        @JsonProperty("zs") Double speed,          // 涨速(%)
        @JsonProperty("fm") Double fiveMinChange,  // 5分钟涨跌幅(%)
        @JsonProperty("zdf60") Double change60d,   // 60日涨跌幅(%)
        @JsonProperty("zdfnc") Double changeYtd,   // 年初至今涨跌幅(%)
        @JsonProperty("t") String updateTime       // 更新时间
) {}
