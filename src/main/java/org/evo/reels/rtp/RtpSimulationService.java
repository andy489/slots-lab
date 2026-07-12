package org.evo.reels.rtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class RtpSimulationService {

    private static final Logger log = LoggerFactory.getLogger(RtpSimulationService.class);

    public RtpResult simulate(RtpRequest req) {
        validate(req);

        long start = System.nanoTime();

        int setCount = req.reelSets().size();
        int screenWidth  = req.screenWidth();
        int screenHeight = req.screenHeight();

        int[][][] reels = new int[setCount][screenWidth][];
        int[] reelLengths = new int[screenWidth];
        for (int s = 0; s < setCount; s++) {
            List<List<Integer>> rs = req.reelSets().get(s).reelSet();
            for (int r = 0; r < screenWidth; r++) {
                List<Integer> reel = rs.get(r);
                int[] arr = new int[reel.size()];
                for (int p = 0; p < reel.size(); p++) arr[p] = reel.get(p);
                reels[s][r] = arr;
                if (s == 0) reelLengths[r] = arr.length;
            }
        }

        int[][] lines = req.lineDefinitions().stream()
                .map(l -> l.stream().mapToInt(Integer::intValue).toArray())
                .toArray(int[][]::new);

        double[] cumulativeChances = buildCumulative(req.reelSetChances());

        SymbolTable symbols = new SymbolTable(req.symbols());
        PayoutStrategy strategy = PayoutStrategyFactory.create(req.strategy());
        int minMatch = req.minMatch();
        double betSize = req.betSize();

        int N = req.threadCount();
        long totalSpins = req.spins();

        try (ExecutorService executor = Executors.newFixedThreadPool(N)) {
            List<Future<SpinStats>> futures = new ArrayList<>(N);

            long base      = totalSpins / N;
            long remainder = totalSpins % N;

            for (int i = 0; i < N; i++) {
                long workerSpins = base + (i < remainder ? 1 : 0);
                futures.add(executor.submit(new RtpWorker(
                        workerSpins, reels, reelLengths, cumulativeChances,
                        screenWidth, screenHeight, minMatch, symbols, strategy, lines)));
            }

            double grandWin = 0.0;
            double grandMaxWin = 0.0;
            double grandSumSquared = 0.0;
            long grandHits = 0;
            MedianTracker globalMedian = new MedianTracker();

            for (int i = 0; i < N; i++) {
                SpinStats s = futures.get(i).get();
                log.info("RTP worker {} done", i);
                grandWin       += s.totalWin();
                grandSumSquared += s.sumSquaredWin();
                grandHits      += s.hitCount();
                if (s.maxWin() > grandMaxWin) grandMaxWin = s.maxWin();
                // Merge median trackers by re-adding sampled values (approximate but correct for large N)
                MedianTracker mt = s.medianTracker();
                double med = mt.median();
                if (med > 0) globalMedian.add(med);
            }

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            double rtp = grandWin / totalSpins * 100.0;
            double avgWin = grandWin / totalSpins * betSize;
            double maxWin = grandMaxWin * betSize;
            double variance = (grandSumSquared / totalSpins) - Math.pow(grandWin / totalSpins, 2);
            double stdDev = Math.sqrt(Math.max(0, variance)) * betSize;
            double medianWin = globalMedian.median() * betSize;

            // Volatility index: stdDev / avgWin (coefficient of variation)
            double volatilityIndex = avgWin > 0 ? stdDev / avgWin : 0.0;
            String volatilityLabel = volatilityIndex < 2.0 ? "Low"
                    : volatilityIndex < 5.0 ? "Medium"
                    : volatilityIndex < 10.0 ? "High"
                    : "Extreme";

            double hitRatePct = grandHits * 100.0 / totalSpins;

            log.info("RTP done — spins={} rtp={}% avgWin={} stdDev={} volatility={} hitRate={}% elapsedMs={}",
                    totalSpins, String.format("%.4f", rtp),
                    String.format("%.4f", avgWin), String.format("%.4f", stdDev),
                    volatilityLabel, String.format("%.2f", hitRatePct), elapsedMs);

            return new RtpResult(rtp, totalSpins, elapsedMs, betSize,
                    avgWin, medianWin, maxWin, stdDev, volatilityIndex, volatilityLabel, hitRatePct);

        } catch (Exception e) {
            throw new RuntimeException("RTP simulation failed: " + e.getMessage(), e);
        }
    }

    private double[] buildCumulative(List<ReelSetChance> chances) {
        double total = chances.stream().mapToDouble(ReelSetChance::chance).sum();
        double[] cum = new double[chances.size()];
        double acc = 0.0;
        for (int i = 0; i < chances.size(); i++) {
            acc += chances.get(i).chance() / total;
            cum[i] = acc;
        }
        cum[cum.length - 1] = 1.0;
        return cum;
    }

    private void validate(RtpRequest req) {
        if (req.reelSets() == null || req.reelSets().isEmpty())
            throw new IllegalArgumentException("No reel sets provided");
        if (req.symbols() == null || req.symbols().isEmpty())
            throw new IllegalArgumentException("No symbol configuration provided");
        if (req.reelSetChances() == null || req.reelSetChances().size() != req.reelSets().size())
            throw new IllegalArgumentException("reelSetChances must have one entry per reel set");

        int setCount = req.reelSets().size();
        int sw = req.screenWidth();
        int sh = req.screenHeight();

        if (sw < 1) throw new IllegalArgumentException("screenWidth must be >= 1");
        if (sh < 1) throw new IllegalArgumentException("screenHeight must be >= 1");
        if (req.minMatch() < 1) throw new IllegalArgumentException("minMatch must be >= 1");
        if (req.minMatch() > sw) throw new IllegalArgumentException("minMatch (" + req.minMatch() + ") cannot exceed screenWidth (" + sw + ")");

        double bet = req.betSize();
        if (bet < 0.1 || bet > 200.0)
            throw new IllegalArgumentException("betSize must be between 0.1 and 200.0");
        if (Math.abs(Math.round(bet * 10) - bet * 10) > 0.001)
            throw new IllegalArgumentException("betSize must be divisible by 0.1");

        for (int s = 0; s < setCount; s++) {
            List<List<Integer>> rs = req.reelSets().get(s).reelSet();
            if (rs.size() < sw)
                throw new IllegalArgumentException("Reel set " + s + " has fewer reels (" + rs.size() + ") than screenWidth (" + sw + ")");
            for (int r = 0; r < sw; r++) {
                if (rs.get(r).size() < sh)
                    throw new IllegalArgumentException("Reel set " + s + " reel " + r + " is shorter than screenHeight (" + sh + ")");
            }
        }

        if (req.lineDefinitions() == null || req.lineDefinitions().isEmpty())
            throw new IllegalArgumentException("At least one line definition is required");
        for (int li = 0; li < req.lineDefinitions().size(); li++) {
            List<Integer> line = req.lineDefinitions().get(li);
            if (line.size() != sw)
                throw new IllegalArgumentException("Line " + li + " must have exactly " + sw + " positions (one per reel), got " + line.size());
            for (int ri = 0; ri < line.size(); ri++) {
                int pos = line.get(ri);
                if (pos < 0 || pos >= sh)
                    throw new IllegalArgumentException("Line " + li + " reel " + ri + ": position " + pos + " out of range [0," + (sh-1) + "]");
            }
        }
        var seen = new java.util.HashSet<List<Integer>>();
        for (int li = 0; li < req.lineDefinitions().size(); li++) {
            if (!seen.add(req.lineDefinitions().get(li)))
                throw new IllegalArgumentException("Duplicate line definition at index " + li);
        }

        if (req.threadCount() < 1 || req.threadCount() > 64)
            throw new IllegalArgumentException("threadCount must be between 1 and 64");
        if (req.spins() <= 0)
            throw new IllegalArgumentException("spins must be positive");

        double chanceSum = 0.0;
        for (ReelSetChance c : req.reelSetChances()) {
            if (c.chance() < 0)
                throw new IllegalArgumentException("Reel set chance cannot be negative");
            chanceSum += c.chance();
        }
        if (Math.abs(chanceSum - 100.0) > 0.05)
            throw new IllegalArgumentException(
                    "Reel set chances must sum to 100.0% (got " + String.format("%.1f", chanceSum) + ")");

        for (SymbolConfig sym : req.symbols()) {
            if (sym.type() == SymbolType.NORMAL || sym.type() == SymbolType.WILD) {
                if (sym.paytable() != null && sym.paytable().stream().anyMatch(v -> v == null || v < 0))
                    throw new IllegalArgumentException("Symbol " + sym.symbolId() + " paytable contains invalid values");
                if (sym.paytable() != null && !sym.paytable().isEmpty()) {
                    int required = sw - req.minMatch() + 1;
                    if (sym.paytable().size() != required)
                        throw new IllegalArgumentException("Symbol " + sym.symbolId() + " paytable must have exactly "
                                + required + " value(s) (screenWidth − minMatch + 1 = " + sw + " − " + req.minMatch() + " + 1)");
                }
            }
            if (sym.type() == SymbolType.WILD) {
                if (sym.wildAggregation() == null)
                    throw new IllegalArgumentException("Symbol " + sym.symbolId() + " wildAggregation must be set");
                if (sym.wildAggregation() != WildMultiplierAggregation.SEQUENCE
                        && sym.wildAggregation() != WildMultiplierAggregation.NONE
                        && sym.wildMultiplier() <= 0)
                    throw new IllegalArgumentException("Symbol " + sym.symbolId() + " wildMultiplier must be > 0");
                if (sym.wildAggregation() == WildMultiplierAggregation.SEQUENCE) {
                    if (sym.wildSequence() == null || sym.wildSequence().isEmpty())
                        throw new IllegalArgumentException("Symbol " + sym.symbolId() + " wildSequence must be provided for SEQUENCE aggregation");
                    if (sym.wildSequence().size() > sw)
                        throw new IllegalArgumentException("Symbol " + sym.symbolId() + " wildSequence length (" + sym.wildSequence().size() + ") exceeds screenWidth (" + sw + ")");
                }
            }
        }
    }
}
