package com.slotslab.rtp;

import java.util.List;

/**
 * @param count         number of spins to generate (1, 3, 10, or 20)
 * @param reelSetIndex  optional — fixed reel set index; null = random weighted pick
 * @param stops         optional — fixed stop positions per reel (one int per reel); null = random
 * @param screen        optional — fixed screen [reel][row]; overrides reelSetIndex + stops
 */
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
        List<List<Integer>> screen
) {}
