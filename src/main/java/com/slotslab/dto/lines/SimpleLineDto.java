package com.slotslab.dto.lines;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.slotslab.dto.spin.PayoutEntry;
import java.util.List;

@JsonPropertyOrder("_className")
public record SimpleLineDto(
        int lineId,
        String matchType,
        int lineSize,
        int lineStart,
        List<Integer> linePos,
        List<Integer> lineDefinition,
        List<Integer> lineSymbols,
        int payoutSymbolId,
        double singularPay,
        double lineMultiplier,
        double winAmount
) implements PayoutEntry {

    @Override
    @JsonProperty("_className")
    public String className() {
        return SimpleLineDto.class.getName();
    }
}
