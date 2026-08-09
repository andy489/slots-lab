package com.slotslab.agent.skills.ltr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Provides two services:
 *
 *  1. seedWeights() — computes an initial probability distribution over all reel sets
 *     based on targetRtp and per-symbol tier/rank. No simulation is run here.
 *
 *  2. simulate() — runs a fast LTR simulation and returns SimStats (rtp, hitRate, etc.).
 *     Called externally by the single-loop orchestrator after each weight/structural change.
 *
 * The convergence loop that previously lived here has been removed. The LLM-driven
 * iterate loop in LtrReelGenerationTool now owns convergence logic.
 */
@Component
public class ReelSetWeightTuner {

    private static final Logger log = LoggerFactory.getLogger(ReelSetWeightTuner.class);
    private static final double MIN_WEIGHT    = 0.001;
    public  static final long   DEFAULT_SPINS = 1_000_000L;

    // ── weight seeding ────────────────────────────────────────────────────────

    /**
     * Seeds initial weights for the combined [noWinSets | winSets] array.
     *
     * No-win share = clamp(1 - targetRtp/100, 0.03, 0.55).
     * Win share distributed by tier + inverse-rank within tier.
     */
    public double[] seedWeights(List<int[][]> noWinSets,
                                 List<int[][]> winSets,
                                 List<SymbolDef> symbols,
                                 double targetRtp) {
        int noWinCount = noWinSets.size();
        int winCount   = winSets.size();
        int n          = noWinCount + winCount;

        double noWinShare = Math.max(0.03, Math.min(0.55, 1.0 - targetRtp / 100.0));
        double winShare   = 1.0 - noWinShare;

        double[] weights = new double[n];
        for (int i = 0; i < noWinCount; i++) {
            weights[i] = noWinShare / noWinCount;
        }

        List<Integer> juniorIds = new ArrayList<>();
        List<Integer> seniorIds = new ArrayList<>();
        for (SymbolDef sym : symbols) {
            if (sym.isJunior()) juniorIds.add(sym.symbolId());
            else if (sym.isSenior()) seniorIds.add(sym.symbolId());
        }
        Collections.sort(juniorIds);
        Collections.sort(seniorIds);

        List<SymbolDef> winSymbols = buildWinSymbolList(symbols, winCount);
        double totalTierWeight = 0;
        double[] tierWeights = new double[winCount];
        for (int i = 0; i < winCount; i++) {
            SymbolDef sym = winSymbols.get(i);
            if (sym == null) { tierWeights[i] = 1.0; totalTierWeight += 1.0; continue; }
            double base = sym.isSenior() ? 0.4 : 1.0;
            List<Integer> ids = sym.isSenior() ? seniorIds : juniorIds;
            tierWeights[i] = base * inverseRankScale(ids.indexOf(sym.symbolId()), ids.size());
            totalTierWeight += tierWeights[i];
        }
        for (int i = 0; i < winCount; i++) {
            weights[noWinCount + i] = winShare * (tierWeights[i] / totalTierWeight);
        }

        normalise(weights);
        log.debug("seedWeights: noWinCount={} winCount={} noWinShare={}", noWinCount, winCount, noWinShare);
        return weights;
    }

    // ── simulation ────────────────────────────────────────────────────────────

    /**
     * Runs a full LTR simulation and returns stats.
     */
    public SimStats simulate(List<int[][]> reelSets, double[] weights,
                              Map<Integer, Map<Integer, Double>> paytable,
                              List<SymbolDef> symbols, List<int[]> lines,
                              int minMatch, int screenWidth, int screenHeight,
                              long spins, long seed) {
        Random rng = new Random(seed);
        double totalWin = 0, sumSqWin = 0, maxWin = 0;
        long hits = 0;
        int n = reelSets.size();

        Map<Integer, Map<Integer, Long>> hitDist = new TreeMap<>();

        int[][] totals = buildTotals(reelSets, n, screenWidth);
        double[] cumW  = buildCumW(weights, n);
        int[][] screen = new int[screenWidth][screenHeight];

        for (long spin = 0; spin < spins; spin++) {
            int si = pickReelSet(rng, cumW, n);
            int[][] rs = reelSets.get(si);

            for (int r = 0; r < screenWidth; r++) {
                int total = totals[si][r];
                for (int row = 0; row < screenHeight; row++) {
                    if (total == 0) { screen[r][row] = 0; continue; }
                    int pick = rng.nextInt(total);
                    int acc = 0;
                    for (int s = 0; s < rs[r].length; s++) {
                        acc += rs[r][s];
                        if (pick < acc) { screen[r][row] = s; break; }
                    }
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
                        if (mult != null) {
                            spinWin += mult;
                            hitDist.computeIfAbsent(symId, k -> new TreeMap<>())
                                   .merge(count, 1L, Long::sum);
                        }
                    }
                }
            }
            if (spinWin > 0) hits++;
            if (spinWin > maxWin) maxWin = spinWin;
            totalWin += spinWin;
            sumSqWin += spinWin * spinWin;
        }

        double rtp    = (totalWin / spins) * 100.0;
        double hr     = (hits / (double) spins) * 100.0;
        double mean   = totalWin / spins;
        double var    = (sumSqWin / spins) - (mean * mean);
        double stdDev = Math.sqrt(Math.max(0, var));
        double volIdx = stdDev / Math.max(mean, 1e-9);
        return new SimStats(rtp, hr, maxWin, stdDev, volIdx, volatilityLabel(volIdx), spins, hitDist);
    }

    public record SimStats(double rtp, double hitRate, double maxWin,
                            double stdDev, double volatilityIndex, String volatilityLabel,
                            long spins, Map<Integer, Map<Integer, Long>> hitDistribution) {}

    // ── normalise ─────────────────────────────────────────────────────────────

    public void normalise(double[] w) {
        double sum = Arrays.stream(w).sum();
        for (int i = 0; i < w.length; i++) w[i] /= sum;
        boolean anyFloored = false;
        for (int i = 0; i < w.length; i++) {
            if (w[i] < MIN_WEIGHT) { w[i] = MIN_WEIGHT; anyFloored = true; }
        }
        if (anyFloored) {
            sum = Arrays.stream(w).sum();
            for (int i = 0; i < w.length; i++) w[i] /= sum;
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<SymbolDef> buildWinSymbolList(List<SymbolDef> symbols, int winCount) {
        List<SymbolDef> list = new ArrayList<>();
        for (int i = 0; i < winCount; i++) list.add(null);
        int wi = 0;
        for (SymbolDef sym : symbols) {
            if (sym.isSpecial()) continue;
            if (wi < winCount) list.set(wi++, sym);
        }
        return list;
    }

    private int[][] buildTotals(List<int[][]> reelSets, int n, int screenWidth) {
        int[][] totals = new int[n][screenWidth];
        for (int si = 0; si < n; si++) {
            int[][] rs = reelSets.get(si);
            for (int r = 0; r < screenWidth; r++) {
                int t = 0;
                for (int c : rs[r]) t += c;
                totals[si][r] = t;
            }
        }
        return totals;
    }

    private double[] buildCumW(double[] weights, int n) {
        double[] cum = new double[n];
        cum[0] = weights[0];
        for (int i = 1; i < n; i++) cum[i] = cum[i - 1] + weights[i];
        return cum;
    }

    private int pickReelSet(Random rng, double[] cumW, int n) {
        double p = rng.nextDouble();
        int si = 0;
        while (si < n - 1 && p > cumW[si]) si++;
        return si;
    }

    private static double inverseRankScale(int rankIdx, int tierSize) {
        if (tierSize <= 1) return 1.0;
        double lo = 0.70, hi = 1.30;
        return hi - (hi - lo) * rankIdx / (tierSize - 1);
    }

    private static String volatilityLabel(double vi) {
        if (vi <  3)  return "Low";
        if (vi <  7)  return "Casual";
        if (vi < 10)  return "High";
        if (vi < 15)  return "Very High";
        if (vi < 25)  return "Extreme";
        return "Ultra Extreme";
    }
}
