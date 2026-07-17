package com.slotslab.simulation.web;

import com.slotslab.simulation.config.AdjacencyOffset;
import com.slotslab.simulation.config.ReelSetChance;
import com.slotslab.simulation.config.ScattersIntervalSet;
import com.slotslab.simulation.config.SymbolConfig;
import com.slotslab.simulation.strategy.PayoutStrategyType;

import java.util.List;

public record SpinTestRequest(
        List<RtpRequest.ReelSetEntry> reelSets,
        List<ReelSetChance> reelSetChances,
        List<SymbolConfig> symbols,
        PayoutStrategyType strategy,
        int screenWidth,
        int screenHeight,
        int minMatch,
        List<List<Integer>> lineDefinitions,
        int count,
        Integer reelSetIndex,
        List<Integer> stops,
        List<List<Integer>> screen,
        List<ScattersIntervalSet> contactsIntervalSets,
        List<AdjacencyOffset> adjacencyOffsets,
        // MEGAWAYS: per-reel-set, per-reel height probabilities (6 values each for heights 2..7)
        List<List<List<Double>>> megawaysReelHeightChances
) {}

