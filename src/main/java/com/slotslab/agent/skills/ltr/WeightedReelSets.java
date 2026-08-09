package com.slotslab.agent.skills.ltr;

import java.util.List;
import java.util.Map;

public record WeightedReelSets(
        List<int[][]> reelSets,
        double[] weights,
        double achievedRtp,
        double achievedHitRate,
        boolean converged,
        SimStats simStats
) {
    public record SimStats(
            double rtp,
            double hitRate,
            double maxWin,
            double stdDev,
            double volatilityIndex,
            String volatilityLabel,
            long spins,
            /** symbolId → matchCount → hit count */
            Map<Integer, Map<Integer, Long>> hitDistribution
    ) {}
}
