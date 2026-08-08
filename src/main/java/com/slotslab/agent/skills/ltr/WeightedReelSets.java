package com.slotslab.agent.skills.ltr;

import java.util.List;

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
            long spins
    ) {}
}
