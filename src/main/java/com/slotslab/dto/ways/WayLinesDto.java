package com.slotslab.dto.ways;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.slotslab.dto.spin.PayoutEntry;
import java.util.List;

@JsonPropertyOrder("_className")
public record WayLinesDto(
        List<WayLineDto> wayLines,
        double globalMultiplier,
        double winAmount
) implements PayoutEntry {

    public static WayLinesDto of(List<WayLineDto> wayLines, double globalMultiplier) {
        double total = wayLines.stream().mapToDouble(WayLineDto::winAmount).sum();
        return new WayLinesDto(wayLines, globalMultiplier, total);
    }

    @Override
    @JsonProperty("_className")
    public String className() {
        return WayLinesDto.class.getName();
    }
}
