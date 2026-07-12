package com.slotslab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonPropertyOrder("_className")
public record SimpleLinesDto(
        String matchType,
        List<SimpleLineDto> lines,
        double winAmount
) implements PayoutEntry {

    public static SimpleLinesDto of(String matchType, List<SimpleLineDto> lines) {
        double total = lines.stream().mapToDouble(SimpleLineDto::winAmount).sum();
        return new SimpleLinesDto(matchType, lines, total);
    }

    @Override
    @JsonProperty("_className")
    public String className() {
        return SimpleLinesDto.class.getName();
    }
}
