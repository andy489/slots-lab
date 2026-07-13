package com.slotslab.simulation.config;

import java.util.List;

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

    public static SymbolConfig normal(int id, List<Double> paytable) {
        return new SymbolConfig(id, SymbolType.NORMAL, paytable, 1.0, WildMultiplierAggregation.ADD, List.of());
    }

    public static SymbolConfig scatter(int id) {
        return new SymbolConfig(id, SymbolType.SCATTER, List.of(), 1.0, WildMultiplierAggregation.ADD, List.of());
    }
}
