package top.xiaocaohub.aichat.stock.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * K线数据（通用格式，适用于 hsstock/history, hsindex/history, bj/history 等）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KlineItem(
        @JsonProperty("t") String time,     // 交易时间
        @JsonProperty("o") Double open,     // 开盘价
        @JsonProperty("h") Double high,     // 最高价
        @JsonProperty("l") Double low,      // 最低价
        @JsonProperty("c") Double close,    // 收盘价
        @JsonProperty("v") Long volume,     // 成交量
        @JsonProperty("a") Double amount,   // 成交额
        @JsonProperty("pc") Double preClose,// 前收盘价
        @JsonProperty("sf") Integer halted  // 停牌 1:停牌 0:正常
) {}
