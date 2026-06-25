package top.xiaocaohub.aichat.stock.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 股票/基金/指数 列表项（dm, mc, jys）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockItem(
        @JsonProperty("dm") String code,
        @JsonProperty("mc") String name,
        @JsonProperty("jys") String exchange
) {}
