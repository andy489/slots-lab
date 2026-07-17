package com.slotslab.simulation.web;

import com.slotslab.simulation.config.AdjacencyOffset;
import com.slotslab.simulation.config.ReelSetChance;
import com.slotslab.simulation.config.ScattersIntervalSet;
import com.slotslab.simulation.config.SymbolConfig;
import com.slotslab.simulation.strategy.PayoutStrategyType;

import java.util.List;

public record RtpRequest(
        List<ReelSetEntry> reelSets,
        List<ReelSetChance> reelSetChances,
        List<SymbolConfig> symbols,
        PayoutStrategyType strategy,
        int screenWidth,
        int screenHeight,
        int minMatch,
        List<List<Integer>> lineDefinitions,
        long spins,
        int threadCount,
        double betSize,
        List<ScattersIntervalSet> contactsIntervalSets,
        List<AdjacencyOffset> adjacencyOffsets,
        // MEGAWAYS: per-reel-set, per-reel height probability distribution.
        // Outer list index = reel-set index; inner list index = reel index;
        // each element is 6 probabilities for heights 2..7 (must sum to 100).
        List<List<List<Double>>> megawaysReelHeightChances
) {
    public record ReelSetEntry(String setName, List<List<Integer>> reelSet) {}
}

