package com.slotlab.reels.rtp;

import java.util.List;

/**
 * @param reelSets          [{setName, reelSet}] — direct output from the Generate tab
 * @param reelSetChances    selection probability per reel set (must sum to 100.0)
 * @param symbols           symbol configurations
 * @param strategy          LTR / RTL / BW
 * @param screenWidth       number of reels visible per spin (must be <= reel count in each set)
 * @param screenHeight      number of rows visible per reel
 * @param minMatch          minimum consecutive matching symbols required for a win
 * @param lineDefinitions   paylines: each line has one row index per reel (0-based)
 * @param spins             total spin count
 * @param threadCount       worker thread count (1–64)
 * @param betSize           stake per spin (0.1–200.0, divisible by 0.1)
 */
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
        double betSize
) {
    public record ReelSetEntry(String setName, List<List<Integer>> reelSet) {}
}
