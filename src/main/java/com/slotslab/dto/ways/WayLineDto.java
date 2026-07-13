package com.slotslab.dto.ways;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.slotslab.dto.spin.PayoutEntry;
import java.util.List;

@JsonPropertyOrder("_className")
public record WayLineDto(
        int floatId,
        int lineSize,
        int lineStart,
        int payoutSymbolId,
        double singularPay,
        double winAmount,
        List<Integer> line1DimPos,
        List<Integer> line1DimSym,
        List<List<Integer>> line2DimPos,
        List<List<Integer>> line2DimSym,
        List<Integer> ways,
        List<Integer> waysWithWaysMultipliers,
        int totalSimpleLines
) implements PayoutEntry {

    @Override
    @JsonProperty("_className")
    public String className() {
        return WayLineDto.class.getName();
    }
}
