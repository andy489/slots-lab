package com.slotslab.dto.spin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonPropertyOrder("_className")
public record SpinData(
        int reelSetIndex,
        List<Integer> reelsStopPositions,
        List<List<Integer>> screen,
        List<Integer> screenSize,
        List<PayoutEntry> payoutData,
        double winAmount,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<Integer> reelHeights,
        String className
) implements PayoutEntry {

    public static SpinData of(
            int reelSetIndex,
            List<Integer> reelsStopPositions,
            List<List<Integer>> screen,
            List<PayoutEntry> payoutData) {
        return of(reelSetIndex, reelsStopPositions, screen, payoutData, null);
    }

    public static SpinData of(
            int reelSetIndex,
            List<Integer> reelsStopPositions,
            List<List<Integer>> screen,
            List<PayoutEntry> payoutData,
            List<Integer> reelHeights) {

        int reels = screen.size();
        int rows  = reels > 0 ? screen.get(0).size() : 0;
        double total = payoutData.stream().mapToDouble(PayoutEntry::winAmount).sum();
        return new SpinData(
                reelSetIndex,
                reelsStopPositions,
                screen,
                List.of(reels, rows),
                payoutData,
                total,
                reelHeights,
                SpinData.class.getName()
        );
    }

    @Override
    @JsonProperty("_className")
    public String className() {
        return SpinData.class.getName();
    }
}
