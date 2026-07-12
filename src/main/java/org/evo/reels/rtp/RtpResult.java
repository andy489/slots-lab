package org.evo.reels.rtp;

public record RtpResult(
        double rtpPercent,
        long totalSpins,
        long elapsedMs,
        double betSize,
        double avgWin,
        double medianWin,
        double maxWin,
        double stdDev,
        double volatilityIndex,
        String volatilityLabel,
        double hitRatePct
) {}
