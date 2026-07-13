package com.slotslab.simulation.stats;

import java.util.Map;

public record SpinStats(
        double totalWin,
        double maxWin,
        double sumSquaredWin,
        long hitCount,
        MedianTracker medianTracker,
        Map<ComboKey, long[]> hitCounts,
        Map<ComboKey, double[]> payouts
) {}
