package com.slotslab.simulation.web;

import com.slotslab.simulation.config.AdjacencyOffset;
import com.slotslab.simulation.config.ScattersIntervalSet;
import com.slotslab.simulation.config.SymbolTable;
import com.slotslab.simulation.eval.ClustersEvaluator;
import com.slotslab.simulation.eval.ScattersEvaluator;
import com.slotslab.simulation.eval.LineEvaluator;
import com.slotslab.simulation.eval.WaysEvaluator;
import com.slotslab.simulation.stats.ComboKey;
import com.slotslab.simulation.stats.MedianTracker;
import com.slotslab.simulation.stats.SpinStats;
import com.slotslab.simulation.strategy.AdjPayoutStrategy;
import com.slotslab.simulation.strategy.BwPayoutStrategy;
import com.slotslab.simulation.strategy.ClustersPayoutStrategy;
import com.slotslab.simulation.strategy.MegawaysPayoutStrategy;
import com.slotslab.simulation.strategy.ScattersPayoutStrategy;
import com.slotslab.simulation.strategy.LtrPayoutStrategy;
import com.slotslab.simulation.strategy.PayoutStrategy;
import com.slotslab.simulation.strategy.RtlPayoutStrategy;
import com.slotslab.simulation.strategy.SlPayoutStrategy;
import com.slotslab.simulation.strategy.WaysPayoutStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

public class RtpWorker implements Callable<SpinStats> {

    private static final int MEGAWAYS_MIN_HEIGHT = 2;
    private static final int MEGAWAYS_MAX_HEIGHT = 7;
    private static final int MEGAWAYS_HEIGHT_OPTIONS = MEGAWAYS_MAX_HEIGHT - MEGAWAYS_MIN_HEIGHT + 1; // 6

    private final long spins;
    private final int[][][] reels;
    private final int[][] reelLengths;
    private final double[] cumulativeChances;
    private final int screenWidth;
    private final int screenHeight;
    private final int minMatch;
    private final SymbolTable symbols;
    private final PayoutStrategy strategy;
    private final int[][] lines;
    private final List<ScattersIntervalSet> contactsIntervalSets;
    private final List<AdjacencyOffset> adjacencyOffsets;
    // MEGAWAYS: cumulative height chances[setIdx][reel][heightIdx] for heights 2..7
    private final double[][][] megawaysCumHeights;

    public RtpWorker(long spins,
                     int[][][] reels,
                     int[][] reelLengths,
                     double[] cumulativeChances,
                     int screenWidth,
                     int screenHeight,
                     int minMatch,
                     SymbolTable symbols,
                     PayoutStrategy strategy,
                     int[][] lines,
                     List<ScattersIntervalSet> contactsIntervalSets,
                     List<AdjacencyOffset> adjacencyOffsets,
                     double[][][] megawaysCumHeights) {
        this.spins = spins;
        this.reels = reels;
        this.reelLengths = reelLengths;
        this.cumulativeChances = cumulativeChances;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.minMatch = minMatch;
        this.symbols = symbols;
        this.strategy = strategy;
        this.lines = lines;
        this.contactsIntervalSets = contactsIntervalSets;
        this.adjacencyOffsets = adjacencyOffsets;
        this.megawaysCumHeights = megawaysCumHeights;
    }

    @Override
    public SpinStats call() {
        Random rng = new Random();
        double totalWin = 0.0;
        double maxWin = 0.0;
        double sumSquared = 0.0;
        long hitCount = 0;
        MedianTracker medianTracker = new MedianTracker();
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();

        boolean isLtr        = strategy instanceof LtrPayoutStrategy;
        boolean isRtl        = strategy instanceof RtlPayoutStrategy;
        boolean isBw         = strategy instanceof BwPayoutStrategy;
        boolean isAdj        = strategy instanceof AdjPayoutStrategy;
        boolean isWays       = strategy instanceof WaysPayoutStrategy;
        boolean isMegaways   = strategy instanceof MegawaysPayoutStrategy;
        boolean isScatters   = strategy instanceof ScattersPayoutStrategy;
        boolean isClusters   = strategy instanceof ClustersPayoutStrategy;
        boolean isSuperLines = strategy instanceof SlPayoutStrategy;

        int[][] screen = new int[screenWidth][screenHeight];

        for (long i = 0; i < spins; i++) {
            int setIdx = pickSet(rng.nextDouble());

            if (isMegaways) {
                // Per-reel: draw visible height, fill symbols, pad remainder with mask (0)
                int[] reelHeights = new int[screenWidth];
                for (int r = 0; r < screenWidth; r++) {
                    reelHeights[r] = pickMegawaysHeight(rng.nextDouble(), setIdx, r);
                }
                for (int r = 0; r < screenWidth; r++) {
                    int len = reelLengths[setIdx][r];
                    int start = len == 0 ? 0 : rng.nextInt(len);
                    int visH = reelHeights[r];
                    int[] col = screen[r];
                    for (int w = 0; w < visH; w++) {
                        col[w] = reels[setIdx][r][(start + w) % len];
                    }
                    for (int w = visH; w < screenHeight; w++) {
                        col[w] = 0; // mask
                    }
                }
                // Evaluate using ways logic; WaysEvaluator already ignores mask (id=0 is unknown → no positions)
                double win = WaysEvaluator.evalWaysTracked(screen, screenWidth, screenHeight, symbols, minMatch, hitMap, payMap);
                totalWin += win;
                sumSquared += win * win;
                if (win > 0) { hitCount++; medianTracker.add(win); }
                if (win > maxWin) maxWin = win;
                continue;
            }

            for (int r = 0; r < screenWidth; r++) {
                int len = reelLengths[setIdx][r];
                int start = len == 0 ? 0 : rng.nextInt(len);
                int[] col = screen[r];
                for (int w = 0; w < screenHeight; w++) {
                    col[w] = reels[setIdx][r][(start + w) % len];
                }
            }

            double win;
            if (isLtr) {
                win = LineEvaluator.evalLtrTracked(screen, screenWidth, symbols, lines, minMatch, hitMap, payMap);
            } else if (isRtl) {
                win = LineEvaluator.evalRtlTracked(screen, screenWidth, symbols, lines, minMatch, hitMap, payMap);
            } else if (isBw) {
                double ltr = LineEvaluator.evalLtrTracked(screen, screenWidth, symbols, lines, minMatch, hitMap, payMap);
                double rtl = LineEvaluator.evalRtlTracked(screen, screenWidth, symbols, lines, minMatch, hitMap, payMap);
                win = ltr + rtl;
            } else if (isAdj) {
                win = LineEvaluator.evalAdjTracked(screen, screenWidth, symbols, lines, minMatch, hitMap, payMap);
            } else if (isWays) {
                win = WaysEvaluator.evalWaysTracked(screen, screenWidth, screenHeight, symbols, minMatch, hitMap, payMap);
            } else if (isScatters) {
                win = ScattersEvaluator.evalScattersTracked(screen, screenWidth, screenHeight, symbols, minMatch, contactsIntervalSets, hitMap, payMap);
            } else if (isClusters) {
                win = ClustersEvaluator.evalClustersTracked(screen, screenWidth, screenHeight, symbols, minMatch, contactsIntervalSets, adjacencyOffsets, hitMap, payMap);
            } else if (isSuperLines) {
                win = LineEvaluator.evalSlTracked(screen, screenWidth, symbols, lines, minMatch, hitMap, payMap);
            } else {
                win = strategy.evaluate(screen, screenWidth, symbols, lines, minMatch);
            }

            totalWin += win;
            sumSquared += win * win;
            if (win > 0) {
                hitCount++;
                medianTracker.add(win);
            }
            if (win > maxWin) maxWin = win;
        }

        return new SpinStats(totalWin, maxWin, sumSquared, hitCount, medianTracker, hitMap, payMap);
    }

    private int pickSet(double u) {
        for (int i = 0; i < cumulativeChances.length - 1; i++) {
            if (u < cumulativeChances[i]) return i;
        }
        return cumulativeChances.length - 1;
    }

    private int pickMegawaysHeight(double u, int setIdx, int reel) {
        if (megawaysCumHeights == null
                || setIdx >= megawaysCumHeights.length
                || megawaysCumHeights[setIdx] == null
                || reel >= megawaysCumHeights[setIdx].length) {
            return MEGAWAYS_MAX_HEIGHT;
        }
        double[] cum = megawaysCumHeights[setIdx][reel];
        for (int i = 0; i < cum.length - 1; i++) {
            if (u < cum[i]) return MEGAWAYS_MIN_HEIGHT + i;
        }
        return MEGAWAYS_MAX_HEIGHT;
    }
}
