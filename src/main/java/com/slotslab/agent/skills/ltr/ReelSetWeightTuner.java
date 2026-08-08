package com.slotslab.agent.skills.ltr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

@Component
public class ReelSetWeightTuner {

    private static final Logger log = LoggerFactory.getLogger(ReelSetWeightTuner.class);
    private static final double MIN_WEIGHT    = 0.001;
    private static final long   DEFAULT_SPINS = 500_000L;
    private static final int    DEFAULT_ITER  = 80;
    // Aggressive initial factor; slows down as we approach target
    private static final double FAST_FACTOR   = 1.30;
    private static final double SLOW_FACTOR   = 1.05;
    private static final double FAST_THRESHOLD = 20.0; // use fast factor when rtp gap > this

    public WeightedReelSets tune(
            List<int[][]> noWinSets,
            List<int[][]> winSets,
            Map<Integer, Map<Integer, Double>> paytable,
            List<SymbolDef> symbols,
            List<int[]> lines,
            int minMatch, int screenWidth, int screenHeight,
            double targetRtp, double rtpDelta,
            double targetHitRate, double hitRateDelta,
            int maxIterations) {
        return tune(noWinSets, winSets, paytable, symbols, lines,
                minMatch, screenWidth, screenHeight,
                targetRtp, rtpDelta, targetHitRate, hitRateDelta,
                maxIterations, () -> false);
    }

    public WeightedReelSets tune(
            List<int[][]> noWinSets,
            List<int[][]> winSets,
            Map<Integer, Map<Integer, Double>> paytable,
            List<SymbolDef> symbols,
            List<int[]> lines,
            int minMatch, int screenWidth, int screenHeight,
            double targetRtp, double rtpDelta,
            double targetHitRate, double hitRateDelta,
            int maxIterations,
            BooleanSupplier cancelCheck) {

        List<int[][]> all = new ArrayList<>();
        all.addAll(noWinSets);
        all.addAll(winSets);

        int n = all.size();
        // Start heavily biased toward winning sets to get RTP moving
        double[] weights = new double[n];
        int noWinCount = noWinSets.size();
        double winShare = 0.80;
        double noWinShare = 0.20;
        for (int i = 0; i < noWinCount; i++)       weights[i] = noWinShare / noWinCount;
        for (int i = noWinCount; i < n; i++)        weights[i] = winShare / (n - noWinCount);

        double bestRtpDist = Double.MAX_VALUE;
        double[] bestWeights = weights.clone();
        double bestRtp = 0, bestHr = 0;
        boolean converged = false;

        for (int iter = 0; iter < maxIterations; iter++) {
            if (cancelCheck.getAsBoolean()) {
                throw new CancellationException("Weight tuning cancelled by user");
            }
            double[] sim = simulate(all, weights, paytable, symbols, lines,
                    minMatch, screenWidth, screenHeight, DEFAULT_SPINS);
            double actualRtp = sim[0];
            double actualHr  = sim[1];

            log.debug("iter={} RTP={} HR={}", iter, actualRtp, actualHr);

            double rtpDist = Math.abs(actualRtp - targetRtp) + Math.abs(actualHr - targetHitRate);
            if (rtpDist < bestRtpDist) {
                bestRtpDist = rtpDist;
                bestWeights = weights.clone();
                bestRtp = actualRtp;
                bestHr  = actualHr;
            }

            boolean rtpOk = Math.abs(actualRtp - targetRtp) <= rtpDelta;
            boolean hrOk  = Math.abs(actualHr  - targetHitRate) <= hitRateDelta;
            if (rtpOk && hrOk) {
                converged = true;
                log.info("Weight tuner converged at iteration {} — RTP={} HR={}", iter, actualRtp, actualHr);
                break;
            }

            double rtpGap = Math.abs(actualRtp - targetRtp);
            double factor = rtpGap > FAST_THRESHOLD ? FAST_FACTOR : SLOW_FACTOR;

            // RTP control
            if (actualRtp < targetRtp - rtpDelta) {
                shiftWeight(weights, n, noWinCount, true, factor);
            } else if (actualRtp > targetRtp + rtpDelta) {
                shiftWeight(weights, n, noWinCount, false, factor);
            }

            // Hit-rate control
            if (actualHr < targetHitRate - hitRateDelta) {
                scaleNoWin(weights, noWinCount, 1.0 / SLOW_FACTOR);
            } else if (actualHr > targetHitRate + hitRateDelta) {
                scaleNoWin(weights, noWinCount, SLOW_FACTOR);
            }

            normalise(weights);
        }

        if (!converged) {
            log.warn("Weight tuner did not converge — returning best weights (RTP={}, HR={})", bestRtp, bestHr);
        }
        double[] finalSim = simulateFull(all, bestWeights, paytable, symbols, lines,
                minMatch, screenWidth, screenHeight, DEFAULT_SPINS);
        WeightedReelSets.SimStats stats = new WeightedReelSets.SimStats(
                round2(finalSim[0]), round2(finalSim[1]),
                round2(finalSim[2]), round2(finalSim[3]),
                round2(finalSim[4]), volatilityLabel(finalSim[4]),
                DEFAULT_SPINS);
        return new WeightedReelSets(all, bestWeights, bestRtp, bestHr, converged, stats);
    }

    // ── weight helpers ────────────────────────────────────────────────────────

    private void shiftWeight(double[] w, int total, int noWinCount, boolean increaseWin, double factor) {
        double winDelta = 0;
        for (int i = noWinCount; i < total; i++) {
            double newW = increaseWin ? w[i] * factor : w[i] / factor;
            winDelta += newW - w[i];
            w[i] = newW;
        }
        double perNoWin = -winDelta / noWinCount;
        for (int i = 0; i < noWinCount; i++) w[i] = Math.max(MIN_WEIGHT, w[i] + perNoWin);
    }

    private void scaleNoWin(double[] w, int noWinCount, double factor) {
        for (int i = 0; i < noWinCount; i++) w[i] = Math.max(MIN_WEIGHT, w[i] * factor);
    }

    private void normalise(double[] w) {
        double sum = Arrays.stream(w).sum();
        for (int i = 0; i < w.length; i++) w[i] = Math.max(MIN_WEIGHT, w[i] / sum);
        sum = Arrays.stream(w).sum();
        for (int i = 0; i < w.length; i++) w[i] /= sum;
    }

    // ── LTR simulator (fast — rtp + hitRate only) ─────────────────────────────

    private double[] simulate(List<int[][]> reelSets, double[] weights,
                               Map<Integer, Map<Integer, Double>> paytable,
                               List<SymbolDef> symbols, List<int[]> lines,
                               int minMatch, int screenWidth, int screenHeight,
                               long spins) {
        double[] full = simulateFull(reelSets, weights, paytable, symbols, lines,
                minMatch, screenWidth, screenHeight, spins);
        return new double[]{full[0], full[1]};
    }

    // ── LTR simulator (full stats: rtp, hitRate, maxWin, stdDev, volatilityIndex) ──

    private double[] simulateFull(List<int[][]> reelSets, double[] weights,
                               Map<Integer, Map<Integer, Double>> paytable,
                               List<SymbolDef> symbols, List<int[]> lines,
                               int minMatch, int screenWidth, int screenHeight,
                               long spins) {
        Random rng = new Random(42);
        double totalWin = 0;
        double sumSqWin  = 0;
        double maxWin    = 0;
        long hits = 0;
        int n = reelSets.size();
        int numSymbols = symbols.size();

        int[][] totals = new int[n][screenWidth];
        for (int si = 0; si < n; si++) {
            int[][] rs = reelSets.get(si);
            for (int r = 0; r < screenWidth; r++) {
                int t = 0;
                for (int c : rs[r]) t += c;
                totals[si][r] = t;
            }
        }

        double[] cumW = new double[n];
        cumW[0] = weights[0];
        for (int i = 1; i < n; i++) cumW[i] = cumW[i - 1] + weights[i];

        int[][] screen = new int[screenWidth][screenHeight];

        for (long spin = 0; spin < spins; spin++) {
            double p = rng.nextDouble();
            int si = 0;
            while (si < n - 1 && p > cumW[si]) si++;
            int[][] rs = reelSets.get(si);

            for (int r = 0; r < screenWidth; r++) {
                int total = totals[si][r];
                for (int row = 0; row < screenHeight; row++) {
                    if (total == 0) { screen[r][row] = 0; continue; }
                    int pick = rng.nextInt(total);
                    int acc = 0;
                    int symIdx = 0;
                    for (int s = 0; s < rs[r].length; s++) {
                        acc += rs[r][s];
                        if (pick < acc) { symIdx = s; break; }
                    }
                    screen[r][row] = symIdx;
                }
            }

            double spinWin = 0;
            for (int[] line : lines) {
                int row0 = (line.length > 0 && line[0] < screenHeight) ? line[0] : 0;
                int firstSymIdx = screen[0][row0];
                int count = 1;
                for (int r = 1; r < screenWidth && r < line.length; r++) {
                    int row = line[r] < screenHeight ? line[r] : 0;
                    if (screen[r][row] == firstSymIdx) count++;
                    else break;
                }
                if (count >= minMatch) {
                    int symId = symbols.get(firstSymIdx).symbolId();
                    Map<Integer, Double> symPay = paytable.get(symId);
                    if (symPay != null) {
                        Double mult = symPay.get(count);
                        if (mult != null) spinWin += mult;
                    }
                }
            }
            if (spinWin > 0) hits++;
            if (spinWin > maxWin) maxWin = spinWin;
            totalWin += spinWin;
            sumSqWin += spinWin * spinWin;
        }

        double rtp  = (totalWin / spins) * 100.0;
        double hr   = (hits / (double) spins) * 100.0;
        double mean = totalWin / spins;
        double variance = (sumSqWin / spins) - (mean * mean);
        double stdDev = Math.sqrt(Math.max(0, variance));
        double volIdx = stdDev / Math.max(mean, 1e-9);
        return new double[]{rtp, hr, maxWin, stdDev, volIdx};
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static String volatilityLabel(double vi) {
        if (vi <  3)  return "Low";
        if (vi <  7)  return "Casual";
        if (vi < 10)  return "High";
        if (vi < 15)  return "Very High";
        if (vi < 25)  return "Extreme";
        return "Ultra Extreme";
    }
}
