package com.slotslab.reels.rtp;

import java.util.List;

/**
 * @param symbolId          1-based symbol ID
 * @param type              NORMAL / WILD / SCATTER
 * @param paytable          payout multipliers indexed 0-based by (matchCount-1);
 *                          e.g. [0, 0, 1.5, 3.0, 10.0] means x1=0, x2=0, x3=1.5, x4=3.0, x5=10.0
 * @param wildMultiplier    multiplier applied when this wild participates in a win (default 1.0, only for WILD)
 * @param wildAggregation   how to combine multiple wilds' multipliers on the same line (only for WILD)
 * @param wildSequence      ordered multipliers indexed by wild count (1-based); used only when wildAggregation=SEQUENCE
 */
public record SymbolConfig(
        int symbolId,
        SymbolType type,
        List<Double> paytable,
        double wildMultiplier,
        WildMultiplierAggregation wildAggregation,
        List<Double> wildSequence
) {
    public SymbolConfig {
        if (type == SymbolType.WILD && wildAggregation == null)
            throw new IllegalArgumentException("Wild symbol " + symbolId + " must have a wildAggregation");
        if (wildSequence == null) wildSequence = List.of();
    }

    /** Convenience constructor for non-wild symbols */
    public static SymbolConfig normal(int id, List<Double> paytable) {
        return new SymbolConfig(id, SymbolType.NORMAL, paytable, 1.0, WildMultiplierAggregation.ADD, List.of());
    }

    public static SymbolConfig scatter(int id) {
        return new SymbolConfig(id, SymbolType.SCATTER, List.of(), 1.0, WildMultiplierAggregation.ADD, List.of());
    }
}
