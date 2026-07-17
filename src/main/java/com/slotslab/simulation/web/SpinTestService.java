package com.slotslab.simulation.web;

import com.slotslab.dto.scatters.ContactsDto;
import com.slotslab.dto.lines.SimpleLineDto;
import com.slotslab.dto.lines.SimpleLinesDto;
import com.slotslab.dto.spin.PayoutEntry;
import com.slotslab.dto.spin.SpinData;
import com.slotslab.dto.ways.WayLinesDto;
import com.slotslab.simulation.config.AdjacencyOffset;
import com.slotslab.simulation.config.ReelSetChance;
import com.slotslab.simulation.config.ScattersIntervalSet;
import com.slotslab.simulation.config.SymbolConfig;
import com.slotslab.simulation.config.SymbolTable;
import com.slotslab.simulation.config.SymbolType;
import com.slotslab.simulation.config.WildMultiplierAggregation;
import com.slotslab.simulation.eval.ClustersEvaluator;
import com.slotslab.simulation.eval.ScattersEvaluator;
import com.slotslab.simulation.eval.WaysEvaluator;
import com.slotslab.simulation.strategy.ClustersPayoutStrategy;
import com.slotslab.simulation.strategy.AdjPayoutStrategy;
import com.slotslab.simulation.strategy.BwPayoutStrategy;
import com.slotslab.simulation.strategy.MegawaysPayoutStrategy;
import com.slotslab.simulation.strategy.ScattersPayoutStrategy;
import com.slotslab.simulation.strategy.LtrPayoutStrategy;
import com.slotslab.simulation.strategy.PayoutStrategy;
import com.slotslab.simulation.strategy.PayoutStrategyFactory;
import com.slotslab.simulation.strategy.PayoutStrategyType;
import com.slotslab.simulation.strategy.RtlPayoutStrategy;
import com.slotslab.simulation.strategy.SlPayoutStrategy;
import com.slotslab.simulation.strategy.WaysPayoutStrategy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class SpinTestService {

    public List<SpinData> generate(SpinTestRequest req) {
        validate(req);

        boolean hasFixedScreen = req.screen() != null && !req.screen().isEmpty();
        int screenWidth  = req.screenWidth();
        int screenHeight = req.screenHeight();

        int[][][] reels;
        double[] cumulative;
        if (hasFixedScreen) {
            reels      = new int[0][][];
            cumulative = new double[]{1.0};
        } else {
            int setCount = req.reelSets().size();
            reels = new int[setCount][screenWidth][];
            for (int s = 0; s < setCount; s++) {
                List<List<Integer>> rs = req.reelSets().get(s).reelSet();
                for (int r = 0; r < screenWidth; r++) {
                    List<Integer> reel = rs.get(r);
                    int[] arr = new int[reel.size()];
                    for (int p = 0; p < reel.size(); p++) arr[p] = reel.get(p);
                    reels[s][r] = arr;
                }
            }
            cumulative = buildCumulative(req.reelSetChances());
        }

        int[][] lines = req.lineDefinitions() != null
                ? req.lineDefinitions().stream().map(l -> l.stream().mapToInt(Integer::intValue).toArray()).toArray(int[][]::new)
                : new int[0][];

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
        List<Integer> reelHeights = null;

        boolean isMegaways = strategy instanceof MegawaysPayoutStrategy;

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

            if (isMegaways) {
                double[][][] cumHeights = buildMegawaysCumHeights(req, screenWidth);
                reelHeights = new ArrayList<>(screenWidth);
                for (int r = 0; r < screenWidth; r++) {
                    int visH = pickMegawaysHeight(rng.nextDouble(), cumHeights, setIdx, r);
                    reelHeights.add(visH);
                    int len = reels[setIdx][r].length;
                    int start = stops.get(r);
                    for (int w = 0; w < visH; w++) {
                        screen[r][w] = reels[setIdx][r][(start + w) % len];
                    }
                    for (int w = visH; w < screenHeight; w++) {
                        screen[r][w] = 0; // mask
                    }
                }
            } else {
                for (int r = 0; r < screenWidth; r++) {
                    int len   = reels[setIdx][r].length;
                    int start = stops.get(r);
                    for (int w = 0; w < screenHeight; w++) {
                        screen[r][w] = reels[setIdx][r][(start + w) % len];
                    }
                }
            }
        }

        List<PayoutEntry> payoutData = evalPerLine(screen, screenWidth, symbols, lines, req.minMatch(), strategy, req.contactsIntervalSets(), req.adjacencyOffsets());

        List<List<Integer>> screenList = new ArrayList<>(screenWidth);
        for (int r = 0; r < screenWidth; r++) {
            List<Integer> col = new ArrayList<>(screenHeight);
            for (int w = 0; w < screenHeight; w++) col.add(screen[r][w]);
            screenList.add(col);
        }

        return SpinData.of(setIdx, stops, screenList, payoutData, reelHeights);
    }

    private double[][][] buildMegawaysCumHeights(SpinTestRequest req, int screenWidth) {
        int setCount = req.reelSets() != null ? req.reelSets().size() : 1;
        double[][][] result = new double[setCount][screenWidth][];
        List<List<List<Double>>> raw = req.megawaysReelHeightChances();
        for (int s = 0; s < setCount; s++) {
            for (int r = 0; r < screenWidth; r++) {
                double[] probs = null;
                if (raw != null && s < raw.size() && raw.get(s) != null
                        && r < raw.get(s).size() && raw.get(s).get(r) != null) {
                    List<Double> p = raw.get(s).get(r);
                    if (p.size() == 6) probs = p.stream().mapToDouble(Double::doubleValue).toArray();
                }
                if (probs == null) probs = new double[]{1, 1, 1, 1, 1, 1};
                double total = 0; for (double v : probs) total += v;
                double[] cum = new double[6]; double acc = 0;
                for (int i = 0; i < 6; i++) { acc += probs[i] / total; cum[i] = acc; }
                cum[5] = 1.0;
                result[s][r] = cum;
            }
        }
        return result;
    }

    private int pickMegawaysHeight(double u, double[][][] cum, int setIdx, int reel) {
        if (cum == null || setIdx >= cum.length || cum[setIdx] == null || reel >= cum[setIdx].length)
            return 7;
        double[] c = cum[setIdx][reel];
        for (int i = 0; i < c.length - 1; i++) { if (u < c[i]) return 2 + i; }
        return 7;
    }

    private List<PayoutEntry> evalPerLine(
            int[][] screen, int screenWidth, SymbolTable symbols,
            int[][] lines, int minMatch, PayoutStrategy strategy,
            List<ScattersIntervalSet> contactsIntervalSets,
            List<AdjacencyOffset> adjacencyOffsets) {

        boolean isLtr        = strategy instanceof LtrPayoutStrategy;
        boolean isRtl        = strategy instanceof RtlPayoutStrategy;
        boolean isBw         = strategy instanceof BwPayoutStrategy;
        boolean isAdj        = strategy instanceof AdjPayoutStrategy;
        boolean isWays       = strategy instanceof WaysPayoutStrategy;
        boolean isMegaways   = strategy instanceof MegawaysPayoutStrategy;
        boolean isScatters   = strategy instanceof ScattersPayoutStrategy;
        boolean isClusters   = strategy instanceof ClustersPayoutStrategy;
        boolean isSuperLines = strategy instanceof SlPayoutStrategy;

        List<PayoutEntry> result = new ArrayList<>();

        if (isLtr || isBw) {
            List<SimpleLineDto> ltrLines = new ArrayList<>();
            for (int li = 0; li < lines.length; li++) {
                SimpleLineDto e = evalSingleLine(screen, screenWidth, symbols, lines[li], minMatch, false, 0, li);
                if (e != null) ltrLines.add(e);
            }
            if (!ltrLines.isEmpty()) result.add(SimpleLinesDto.of("LTR", ltrLines));
        }
        if (isRtl || isBw) {
            List<SimpleLineDto> rtlLines = new ArrayList<>();
            for (int li = 0; li < lines.length; li++) {
                SimpleLineDto e = evalSingleLine(screen, screenWidth, symbols, lines[li], minMatch, true, 0, li);
                if (e != null) rtlLines.add(e);
            }
            if (!rtlLines.isEmpty()) result.add(SimpleLinesDto.of("RTL", rtlLines));
        }
        if (isAdj) {
            List<SimpleLineDto> adjLines = new ArrayList<>();
            for (int li = 0; li < lines.length; li++) {
                SimpleLineDto e = evalAdjSingleLine(screen, screenWidth, symbols, lines[li], minMatch, li);
                if (e != null) adjLines.add(e);
            }
            if (!adjLines.isEmpty()) result.add(SimpleLinesDto.of("ADJ", adjLines));
        }
        if (isWays) {
            int screenHeight = screen[0].length;
            WayLinesDto wayLines = WaysEvaluator.evalWays(screen, screenWidth, screenHeight, symbols, minMatch);
            if (wayLines != null) result.add(wayLines);
        }
        if (isMegaways) {
            // screen already has mask symbols (0) in padded positions; evalWays ignores them
            int screenHeight = screen[0].length;
            WayLinesDto wayLines = WaysEvaluator.evalWays(screen, screenWidth, screenHeight, symbols, minMatch);
            if (wayLines != null) result.add(wayLines);
        }
        if (isScatters) {
            int screenHeight = screen[0].length;
            ContactsDto contacts = ScattersEvaluator.evalScatters(screen, screenWidth, screenHeight, symbols, minMatch, contactsIntervalSets);
            if (contacts != null) result.add(contacts);
        }
        if (isClusters) {
            int screenHeight = screen[0].length;
            ContactsDto clusters = ClustersEvaluator.evalClusters(screen, screenWidth, screenHeight, symbols, minMatch, contactsIntervalSets, adjacencyOffsets);
            if (clusters != null) result.add(clusters);
        }
        if (isSuperLines) {
            List<SimpleLineDto> superLines = new ArrayList<>();
            for (int li = 0; li < lines.length; li++) {
                SimpleLineDto e = evalSuperLineSingle(screen, screenWidth, symbols, lines[li], minMatch, li);
                if (e != null) superLines.add(e);
            }
            if (!superLines.isEmpty()) result.add(SimpleLinesDto.of("SL", superLines));
        }
        return result;
    }

    private SimpleLineDto evalSuperLineSingle(
            int[][] screen, int reelCount, SymbolTable symbols,
            int[] line, int minMatch, int lineIdx) {

        int totalWilds = 0;
        Integer wildItem = null;
        double lineMultiplier = 0.0;
        java.util.Map<Integer, Integer> symCounts = new java.util.LinkedHashMap<>();

        for (int reel = 0; reel < reelCount; reel++) {
            int sym = screen[reel][line[reel]];

            if (symbols.isScatter(sym)) break;

            boolean isWild = symbols.isWild(sym);

            if (isWild) {
                totalWilds++;
                wildItem = sym;
                SymbolConfig wc = symbols.get(sym);
                if (wc != null && wc.wildAggregation() != WildMultiplierAggregation.NONE) {
                    double wm = wc.wildMultiplier();
                    switch (wc.wildAggregation()) {
                        case ADD      -> lineMultiplier += wm;
                        case MULTIPLY -> lineMultiplier = (lineMultiplier == 0.0) ? wm : lineMultiplier * wm;
                        default       -> {}
                    }
                }
            } else {
                symCounts.merge(sym, 1, Integer::sum);
            }
        }

        if (lineMultiplier <= 0.0) lineMultiplier = 1.0;

        // Find the normal symbol that gives the best win (own count + wilds)
        double bestWeight = 0.0;
        Integer bestSym = null;
        int bestMatchCount = 0;

        for (java.util.Map.Entry<Integer, Integer> entry : symCounts.entrySet()) {
            int sym = entry.getKey();
            int matchCount = entry.getValue() + totalWilds;
            if (matchCount < minMatch) continue;
            List<Double> pt = symbols.get(sym) != null ? symbols.get(sym).paytable() : null;
            if (pt == null) continue;
            int idx = matchCount - minMatch;
            if (idx < 0 || idx >= pt.size()) continue;
            double w = Math.max(0, pt.get(idx));
            if (w > bestWeight) { bestWeight = w; bestSym = sym; bestMatchCount = matchCount; }
        }

        // All-wild fallback
        if (symCounts.isEmpty() && wildItem != null && totalWilds >= minMatch) {
            List<Double> pt = symbols.get(wildItem) != null ? symbols.get(wildItem).paytable() : null;
            double wildWeight = 0.0;
            if (pt != null) {
                int idx = totalWilds - minMatch;
                if (idx >= 0 && idx < pt.size()) wildWeight = Math.max(0, pt.get(idx));
            }
            if (wildWeight == 0.0) {
                for (SymbolConfig sc : symbols.all()) {
                    if (sc.type() == SymbolType.NORMAL) {
                        List<Double> spt = sc.paytable();
                        if (spt != null) {
                            int idx = totalWilds - minMatch;
                            if (idx >= 0 && idx < spt.size()) {
                                double v = Math.max(0, spt.get(idx));
                                if (v > wildWeight) wildWeight = v;
                            }
                        }
                    }
                }
            }
            if (wildWeight > bestWeight) { bestWeight = wildWeight; bestSym = wildItem; bestMatchCount = totalWilds; }
        }

        if (bestWeight == 0.0 || bestSym == null) return null;

        double win = Math.round(bestWeight * lineMultiplier * 100.0) / 100.0;
        if (win == 0.0) return null;

        List<Integer> lineDefinition = new ArrayList<>(line.length);
        for (int v : line) lineDefinition.add(v);

        List<Integer> lineSymbols = new ArrayList<>(reelCount);
        for (int reel = 0; reel < reelCount; reel++) lineSymbols.add(screen[reel][line[reel]]);

        return new SimpleLineDto(
                lineIdx, "SL", bestMatchCount, 0,
                lineDefinition, lineDefinition, lineSymbols,
                bestSym,
                Math.round(bestWeight * 100.0) / 100.0, lineMultiplier, win);
    }

    private SimpleLineDto evalAdjSingleLine(            int[][] screen, int reelCount, SymbolTable symbols,
            int[] line, int minMatch, int lineIdx) {

        SimpleLineDto best = null;
        int maxStart = reelCount - minMatch;
        for (int startReel = 0; startReel <= maxStart; startReel++) {
            SimpleLineDto candidate = evalSingleLine(screen, reelCount, symbols, line, minMatch, false, startReel, lineIdx);
            if (candidate != null && (best == null || candidate.winAmount() > best.winAmount())) {
                best = candidate;
            }
        }
        return best;
    }

    private SimpleLineDto evalSingleLine(
            int[][] screen, int reelCount, SymbolTable symbols,
            int[] line, int minMatch, boolean reversed, int startReel, int lineIdx) {

        int streak = 0, wildStreak = 0;
        Integer prevSym = null, currSym;
        boolean allWild = true;
        Integer payoutSymbolId = null, wildItem = null;
        double lineMultiplier = 0.0;

        for (int ri = startReel; ri < reelCount; ri++) {
            int r = reversed ? (reelCount - 1 - (ri - startReel)) : ri;
            currSym = screen[r][line[r]];

            if (symbols.isScatter(currSym)) {
                if (ri == startReel) return null;
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

        double weight = 0.0;
        if (payoutSymbolId != null && streak >= minMatch) {
            List<Double> pt = symbols.get(payoutSymbolId) != null ? symbols.get(payoutSymbolId).paytable() : null;
            if (pt != null && (streak - minMatch) < pt.size()) weight = Math.max(0, pt.get(streak - minMatch));
        }

        double wildWeight = 0.0;
        if (wildItem != null && wildStreak >= minMatch) {
            List<Double> pt = symbols.get(wildItem) != null ? symbols.get(wildItem).paytable() : null;
            if (pt != null && (wildStreak - minMatch) < pt.size()) wildWeight = Math.max(0, pt.get(wildStreak - minMatch));
            if (wildWeight == 0.0 && prevSym == null) {
                for (SymbolConfig sc : symbols.all()) {
                    if (sc.type() == SymbolType.NORMAL) {
                        List<Double> spt = sc.paytable();
                        if (spt != null && (wildStreak - minMatch) < spt.size()) {
                            double v = Math.max(0, spt.get(wildStreak - minMatch));
                            if (v > wildWeight) wildWeight = v;
                        }
                    }
                }
            }
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

        String matchType;
        int lineStart;
        if (startReel > 0) {
            matchType = "ADJ";
            lineStart = startReel;
        } else if (reversed) {
            matchType = "RTL";
            lineStart = reelCount - streak;
        } else {
            matchType = "LTR";
            lineStart = 0;
        }

        List<Integer> lineDefinition = new ArrayList<>(line.length);
        for (int v : line) lineDefinition.add(v);

        List<Integer> linePos     = new ArrayList<>(streak);
        List<Integer> lineSymbols = new ArrayList<>(streak);
        for (int i = 0; i < streak; i++) {
            int r = reversed ? (reelCount - 1 - i) : (startReel + i);
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
        boolean hasFixedScreen = req.screen() != null && !req.screen().isEmpty();

        if (!hasFixedScreen) {
            if (req.reelSets() == null || req.reelSets().isEmpty())
                throw new IllegalArgumentException("No reel sets provided");
            if (req.reelSetChances() == null || req.reelSetChances().size() != req.reelSets().size())
                throw new IllegalArgumentException("reelSetChances must have one entry per reel set");
        }
        if (req.symbols() == null || req.symbols().isEmpty())
            throw new IllegalArgumentException("No symbol configuration provided");
        if (req.screenWidth() < 1)
            throw new IllegalArgumentException("screenWidth must be >= 1");
        if (req.screenHeight() < 1)
            throw new IllegalArgumentException("screenHeight must be >= 1");
        if (req.strategy() != PayoutStrategyType.WAYS &&
                req.strategy() != PayoutStrategyType.MEGAWAYS &&
                req.strategy() != PayoutStrategyType.SCATTERS &&
                req.strategy() != PayoutStrategyType.CLUSTERS &&
                (req.lineDefinitions() == null || req.lineDefinitions().isEmpty()))
            throw new IllegalArgumentException("At least one line definition is required");        if (!hasFixedScreen && req.reelSetIndex() != null) {
            int idx = req.reelSetIndex();
            if (idx < 0 || idx >= req.reelSets().size())
                throw new IllegalArgumentException("reelSetIndex " + idx + " out of range");
        }
        if (req.screen() != null && !req.screen().isEmpty()) {
            if (req.screen().size() < req.screenWidth())
                throw new IllegalArgumentException("Provided screen has fewer columns than screenWidth");
        }
        if ((req.strategy() == PayoutStrategyType.WAYS || req.strategy() == PayoutStrategyType.MEGAWAYS)
                && req.symbols() != null) {
            for (SymbolConfig sym : req.symbols()) {
                if (sym.type() == SymbolType.WILD && sym.wildAggregation() != WildMultiplierAggregation.NONE)
                    throw new IllegalArgumentException(
                            req.strategy() + " strategy does not support wild multipliers — symbol " + sym.symbolId() + " must use wildAggregation=NONE");
            }
        }
        if (req.strategy() == PayoutStrategyType.SCATTERS) {
            RtpSimulationService.validateContactsIntervalSets(
                    req.contactsIntervalSets(), req.minMatch(), req.screenWidth() * req.screenHeight());
        }
        if (req.strategy() == PayoutStrategyType.CLUSTERS) {
            RtpSimulationService.validateContactsIntervalSets(
                    req.contactsIntervalSets(), req.minMatch(), req.screenWidth() * req.screenHeight());
        }
        if (req.symbols() != null) {
            for (SymbolConfig sym : req.symbols()) {
                if ((sym.type() == SymbolType.NORMAL || sym.type() == SymbolType.WILD)
                        && sym.paytable() != null && sym.paytable().size() > 1) {
                    List<Double> pt = sym.paytable();
                    for (int i = 1; i < pt.size(); i++) {
                        if (pt.get(i) < pt.get(i - 1))
                            throw new IllegalArgumentException("Symbol " + sym.symbolId()
                                    + " paytable must be non-decreasing: entry[" + i + "]=" + pt.get(i)
                                    + " < entry[" + (i-1) + "]=" + pt.get(i-1));
                    }
                }
            }
        }
    }
}
