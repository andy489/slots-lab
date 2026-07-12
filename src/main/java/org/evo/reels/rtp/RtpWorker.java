package org.evo.reels.rtp;

import java.util.Random;
import java.util.concurrent.Callable;

public class RtpWorker implements Callable<SpinStats> {

    private final long spins;
    private final int[][][] reels;
    private final int[] reelLengths;
    private final double[] cumulativeChances;
    private final int screenWidth;
    private final int screenHeight;
    private final int minMatch;
    private final SymbolTable symbols;
    private final PayoutStrategy strategy;
    private final int[][] lines;

    public RtpWorker(long spins,
                     int[][][] reels,
                     int[] reelLengths,
                     double[] cumulativeChances,
                     int screenWidth,
                     int screenHeight,
                     int minMatch,
                     SymbolTable symbols,
                     PayoutStrategy strategy,
                     int[][] lines) {
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
    }

    @Override
    public SpinStats call() {
        Random rng = new Random();
        double totalWin = 0.0;
        double maxWin = 0.0;
        double sumSquared = 0.0;
        long hitCount = 0;
        MedianTracker medianTracker = new MedianTracker();

        int[][] screen = new int[screenWidth][screenHeight];

        for (long i = 0; i < spins; i++) {
            int setIdx = pickSet(rng.nextDouble());

            for (int r = 0; r < screenWidth; r++) {
                int len = reelLengths[r];
                int start = len == 0 ? 0 : rng.nextInt(len);
                int[] col = screen[r];
                for (int w = 0; w < screenHeight; w++) {
                    col[w] = reels[setIdx][r][(start + w) % len];
                }
            }

            double win = strategy.evaluate(screen, screenWidth, symbols, lines, minMatch);
            totalWin += win;
            sumSquared += win * win;
            if (win > 0) {
                hitCount++;
                medianTracker.add(win);
            }
            if (win > maxWin) maxWin = win;
        }

        return new SpinStats(totalWin, maxWin, sumSquared, hitCount, medianTracker);
    }

    private int pickSet(double u) {
        for (int i = 0; i < cumulativeChances.length - 1; i++) {
            if (u < cumulativeChances[i]) return i;
        }
        return cumulativeChances.length - 1;
    }
}
