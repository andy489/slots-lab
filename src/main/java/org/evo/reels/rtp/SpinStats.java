package org.evo.reels.rtp;

public record SpinStats(
        double totalWin,
        double maxWin,
        double sumSquaredWin,
        long hitCount,
        MedianTracker medianTracker
) {}
