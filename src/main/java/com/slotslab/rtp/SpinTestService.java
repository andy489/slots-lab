package com.slotslab.rtp;

import org.springframework.stereotype.Service;

import com.slotslab.dto.PayoutEntry;
import com.slotslab.dto.SimpleLineDto;
import com.slotslab.dto.SimpleLinesDto;
import com.slotslab.dto.SpinData;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class SpinTestService {

    public List<SpinData> generate(SpinTestRequest req) {
        validate(req);

        int setCount     = req.reelSets().size();
        int screenWidth  = req.screenWidth();
        int screenHeight = req.screenHeight();

        int[][][] reels = new int[setCount][screenWidth][];
        for (int s = 0; s < setCount; s++) {
            List<List<Integer>> rs = req.reelSets().get(s).reelSet();
            for (int r = 0; r < screenWidth; r++) {
                List<Integer> reel = rs.get(r);
                int[] arr = new int[reel.size()];
                for (int p = 0; p < reel.size(); p++) arr[p] = reel.get(p);
                reels[s][r] = arr;
            }
        }

        int[][] lines = req.lineDefinitions().stream()
                .map(l -> l.stream().mapToInt(Integer::intValue).toArray())
                .toArray(int[][]::new);

        double[] cumulative = buildCumulative(req.reelSetChances());
        SymbolTable symbols  = new SymbolTable(req.symbols());
        PayoutStrategy strategy = PayoutStrategyFactory.create(req.strategy());

        Random rng = new Random();
        int count  = Math.max(1, req.count());

        List<SpinData> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            results.add(evalOneSpin(req, reels, lines, cumulative, symbols, strategy, rng, screenWidth, screenHeight));
        }
        return results;
    }

    private SpinData evalOneSpin(
            SpinTestRequest req,
            int[][][] reels,
            int[][] lines,
            double[] cumulative,
            SymbolTable symbols,
            PayoutStrategy strategy,
            Random rng,
            int screenWidth,
            int screenHeight) {

        int setIdx;
        List<Integer> stops;
        int[][] screen = new int[screenWidth][screenHeight];

        if (req.screen() != null && !req.screen().isEmpty()) {
            setIdx = -1;
            stops  = List.of();
            for (int r = 0; r < Math.min(screenWidth, req.screen().size()); r++) {
                List<Integer> col = req.screen().get(r);
                for (int w = 0; w < Math.min(screenHeight, col.size()); w++) {
                    screen[r][w] = col.get(w);
                }
            }
        } else {
            if (req.reelSetIndex() != null) {
                setIdx = req.reelSetIndex();
            } else {
                setIdx = pickSet(rng.nextDouble(), cumulative);
            }

            if (req.stops() != null && req.stops().size() == screenWidth) {
                stops = req.stops();
            } else {
                List<Integer> generated = new ArrayList<>(screenWidth);
                for (int r = 0; r < screenWidth; r++) {
                    int len = reels[setIdx][r].length;
                    generated.add(len == 0 ? 0 : rng.nextInt(len));
                }
                stops = generated;
            }

            for (int r = 0; r < screenWidth; r++) {
                int len   = reels[setIdx][r].length;
                int start = stops.get(r);
                for (int w = 0; w < screenHeight; w++) {
                    screen[r][w] = reels[setIdx][r][(start + w) % len];
                }
            }
        }

        List<PayoutEntry> payoutData = evalPerLine(screen, screenWidth, symbols, lines, req.minMatch(), strategy);

        List<List<Integer>> screenList = new ArrayList<>(screenWidth);
        for (int r = 0; r < screenWidth; r++) {
            List<Integer> col = new ArrayList<>(screenHeight);
            for (int w = 0; w < screenHeight; w++) col.add(screen[r][w]);
            screenList.add(col);
        }

        return SpinData.of(setIdx, stops, screenList, payoutData);
    }

    private List<PayoutEntry> evalPerLine(
            int[][] screen, int screenWidth, SymbolTable symbols,
            int[][] lines, int minMatch, PayoutStrategy strategy) {

        boolean isLtr = strategy instanceof LtrPayoutStrategy;
        boolean isRtl = strategy instanceof RtlPayoutStrategy;
        boolean isBw  = strategy instanceof BwPayoutStrategy;

        List<PayoutEntry> result = new ArrayList<>();

        if (isLtr || isBw) {
            List<SimpleLineDto> ltrLines = new ArrayList<>();
            for (int li = 0; li < lines.length; li++) {
                SimpleLineDto e = evalSingleLine(screen, screenWidth, symbols, lines[li], minMatch, false, li);
                if (e != null) ltrLines.add(e);
            }
            if (!ltrLines.isEmpty()) result.add(SimpleLinesDto.of("LTR", ltrLines));
        }
        if (isRtl || isBw) {
            List<SimpleLineDto> rtlLines = new ArrayList<>();
            for (int li = 0; li < lines.length; li++) {
                SimpleLineDto e = evalSingleLine(screen, screenWidth, symbols, lines[li], minMatch, true, li);
                if (e != null) rtlLines.add(e);
            }
            if (!rtlLines.isEmpty()) result.add(SimpleLinesDto.of("RTL", rtlLines));
        }
        return result;
    }

    private SimpleLineDto evalSingleLine(
            int[][] screen, int reelCount, SymbolTable symbols,
            int[] line, int minMatch, boolean reversed, int lineIdx) {

        int streak = 0, wildStreak = 0;
        Integer prevSym = null, currSym;
        boolean allWild = true;
        Integer payoutSymbolId = null, wildItem = null;
        double lineMultiplier = 0.0;

        for (int reel = 0; reel < reelCount; reel++) {
            int r = reversed ? (reelCount - 1 - reel) : reel;
            currSym = screen[r][line[r]];

            if (symbols.isScatter(currSym)) {
                if (reel == 0) return null;
                break;
            }

            boolean isWild = symbols.isWild(currSym);
            if (prevSym == null && !isWild) prevSym = currSym;

            if (allWild || isWild || currSym.equals(prevSym)) {
                streak++;

                if (allWild && isWild) {
                    wildStreak++;
                    wildItem = currSym;
                }

                if (!isWild) {
                    allWild = false;
                } else {
                    SymbolConfig wc = symbols.get(currSym);
                    if (wc != null && wc.wildAggregation() != WildMultiplierAggregation.NONE) {
                        double wm = wc.wildMultiplier();
                        switch (wc.wildAggregation()) {
                            case ADD      -> lineMultiplier += wm;
                            case MULTIPLY -> lineMultiplier = (lineMultiplier == 0.0) ? wm : lineMultiplier * wm;
                            default       -> {}
                        }
                    }
                }
            } else {
                payoutSymbolId = prevSym;
                break;
            }
        }

        if (payoutSymbolId == null && prevSym != null) payoutSymbolId = prevSym;
        if (lineMultiplier <= 0.0) lineMultiplier = 1.0;

        // regular symbol weight
        double weight = 0.0;
        if (payoutSymbolId != null && streak >= minMatch) {
            List<Double> pt = symbols.get(payoutSymbolId) != null ? symbols.get(payoutSymbolId).paytable() : null;
            if (pt != null && (streak - minMatch) < pt.size()) weight = Math.max(0, pt.get(streak - minMatch));
        }

        // wild-only weight
        double wildWeight = 0.0;
        if (wildItem != null && wildStreak >= minMatch) {
            List<Double> pt = symbols.get(wildItem) != null ? symbols.get(wildItem).paytable() : null;
            if (pt != null && (wildStreak - minMatch) < pt.size()) wildWeight = Math.max(0, pt.get(wildStreak - minMatch));
        }

        if (weight == 0.0 && wildWeight == 0.0) return null;
        if (streak < minMatch && wildStreak < minMatch) return null;

        if (wildWeight > weight) {
            payoutSymbolId = wildItem;
            streak         = wildStreak;
            weight         = wildWeight;
        }

        if (streak < minMatch) return null;

        double singularPay = weight;
        double win = Math.round(singularPay * lineMultiplier * 100.0) / 100.0;
        if (win == 0.0) return null;

        String matchType  = reversed ? "RTL" : "LTR";
        int lineStart     = reversed ? (reelCount - streak) : 0;

        List<Integer> lineDefinition = new ArrayList<>(line.length);
        for (int v : line) lineDefinition.add(v);

        // linePos = row indices from line definition for matched positions (mirroring reference subList(0, streak))
        List<Integer> linePos     = new ArrayList<>(streak);
        List<Integer> lineSymbols = new ArrayList<>(streak);
        for (int i = 0; i < streak; i++) {
            int r = reversed ? (reelCount - 1 - i) : i;
            linePos.add(line[r]);
            lineSymbols.add(screen[r][line[r]]);
        }
        if (reversed) {
            java.util.Collections.reverse(linePos);
            java.util.Collections.reverse(lineSymbols);
        }

        double sp = Math.round(singularPay * 100.0) / 100.0;
        return new SimpleLineDto(
                lineIdx, matchType, streak, lineStart,
                linePos, lineDefinition, lineSymbols,
                payoutSymbolId, sp, lineMultiplier, win);
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

    private int pickSet(double u, double[] cumulative) {
        for (int i = 0; i < cumulative.length - 1; i++) {
            if (u < cumulative[i]) return i;
        }
        return cumulative.length - 1;
    }

    private void validate(SpinTestRequest req) {
        if (req.reelSets() == null || req.reelSets().isEmpty())
            throw new IllegalArgumentException("No reel sets provided");
        if (req.symbols() == null || req.symbols().isEmpty())
            throw new IllegalArgumentException("No symbol configuration provided");
        if (req.reelSetChances() == null || req.reelSetChances().size() != req.reelSets().size())
            throw new IllegalArgumentException("reelSetChances must have one entry per reel set");
        if (req.screenWidth() < 1)
            throw new IllegalArgumentException("screenWidth must be >= 1");
        if (req.screenHeight() < 1)
            throw new IllegalArgumentException("screenHeight must be >= 1");
        if (req.lineDefinitions() == null || req.lineDefinitions().isEmpty())
            throw new IllegalArgumentException("At least one line definition is required");
        if (req.reelSetIndex() != null) {
            int idx = req.reelSetIndex();
            if (idx < 0 || idx >= req.reelSets().size())
                throw new IllegalArgumentException("reelSetIndex " + idx + " out of range");
        }
        if (req.screen() != null && !req.screen().isEmpty()) {
            if (req.screen().size() < req.screenWidth())
                throw new IllegalArgumentException("Provided screen has fewer columns than screenWidth");
        }
    }
}
