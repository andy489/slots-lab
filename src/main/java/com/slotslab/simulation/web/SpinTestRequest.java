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
        List<AdjacencyOffset> adjacencyOffsets
) {}

