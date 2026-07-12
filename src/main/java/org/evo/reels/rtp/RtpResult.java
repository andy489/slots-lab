package org.evo.reels.rtp;

import java.util.List;

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
        double hitRatePct,
        List<ComboStats> comboBreakdown
) {}
