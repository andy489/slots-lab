package com.slotslab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Result for one generated spin.
 *
 * @param reelSetIndex       which reel set was used (-1 for fixed screen)
 * @param reelsStopPositions stop positions (top-of-screen index per reel)
 * @param screen             2-D symbol grid [reel][row]
 * @param screenSize         [reels, rows]
 * @param payoutData         winning combinations produced by the evaluation
 * @param winAmount          total win across all payout entries
 * @param className          fully-qualified class name of this type
 */
@JsonPropertyOrder("_className")
public record SpinData(
        int reelSetIndex,
        List<Integer> reelsStopPositions,
        List<List<Integer>> screen,
        List<Integer> screenSize,
        List<PayoutEntry> payoutData,
        double winAmount,
        String className
) implements PayoutEntry {

    public static SpinData of(
            int reelSetIndex,
            List<Integer> reelsStopPositions,
            List<List<Integer>> screen,
            List<PayoutEntry> payoutData) {

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
                SpinData.class.getName()
        );
    }

    @Override
    @JsonProperty("_className")
    public String className() {
        return SpinData.class.getName();
    }
}
