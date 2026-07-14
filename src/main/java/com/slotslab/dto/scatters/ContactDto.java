package com.slotslab.dto.scatters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.slotslab.dto.spin.PayoutEntry;

import java.util.List;

@JsonPropertyOrder("_className")
public record ContactDto(
        int floatId,
        int contactSize,
        int payoutSymbolId,
        int contactStartReel,
        int contactStartRow,
        double localMultiplier,
        double singularPayout,
        double winAmount,
        List<Integer> contact1DimPos,
        List<Integer> contact1DimSym,
        List<List<Integer>> contact2DimPos,
        List<List<Integer>> contact2DimSym
) implements PayoutEntry {

    @Override
    @JsonProperty("_className")
    public String className() {
        return ContactDto.class.getName();
    }
}
