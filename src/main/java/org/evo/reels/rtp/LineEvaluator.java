package org.evo.reels.rtp;

import java.util.List;
import java.util.Map;

/**
 * Core line-based win evaluator.
 *
 * screen[reel][row] — primitive arrays for performance.
 * lines[lineIdx][reelIdx] = row position (0-based).
 */
final class LineEvaluator {

    private LineEvaluator() {}

    private record LineResult(double win, int paySymbol, int streak) {}

    // ── untracked (fast path) ────────────────────────────────────────────────

    static double evalLtr(int[][] screen, int reelCount, SymbolTable symbols, int[][] lines, int minMatch) {
        double total = 0.0;
        for (int[] line : lines) total += evalLineResult(screen, reelCount, symbols, line, false, minMatch).win();
        return total;
    }

    static double evalRtl(int[][] screen, int reelCount, SymbolTable symbols, int[][] lines, int minMatch) {
        double total = 0.0;
        for (int[] line : lines) total += evalLineResult(screen, reelCount, symbols, line, true, minMatch).win();
        return total;
    }

    // ── tracked variants ─────────────────────────────────────────────────────

    static double evalLtrTracked(int[][] screen, int reelCount, SymbolTable symbols,
                                 int[][] lines, int minMatch,
                                 Map<ComboKey, long[]> hitMap, Map<ComboKey, double[]> payMap) {
        double total = 0.0;
        for (int[] line : lines) {
            LineResult r = evalLineResult(screen, reelCount, symbols, line, false, minMatch);
            total += r.win();
            if (r.win() > 0 && r.paySymbol() >= 0)
                record(r.paySymbol(), r.streak(), r.win(), hitMap, payMap);
        }
        return total;
    }

    static double evalRtlTracked(int[][] screen, int reelCount, SymbolTable symbols,
                                 int[][] lines, int minMatch,
                                 Map<ComboKey, long[]> hitMap, Map<ComboKey, double[]> payMap) {
        double total = 0.0;
        for (int[] line : lines) {
            LineResult r = evalLineResult(screen, reelCount, symbols, line, true, minMatch);
            total += r.win();
            if (r.win() > 0 && r.paySymbol() >= 0)
                record(r.paySymbol(), r.streak(), r.win(), hitMap, payMap);
        }
        return total;
    }

    private static void record(int sym, int streak, double win,
                               Map<ComboKey, long[]> hitMap, Map<ComboKey, double[]> payMap) {
        ComboKey key = new ComboKey(sym, streak);
        hitMap.computeIfAbsent(key, k -> new long[1])[0]++;
        payMap.computeIfAbsent(key, k -> new double[1])[0] += win;
    }

    // ── core evaluator ───────────────────────────────────────────────────────

    /**
     * Evaluates a single payline in one direction.
     *
     * Wild multiplier aggregation rules:
     *   ADD      — sum all wild multipliers on the matching streak; apply sum as multiplier to base win.
     *   MULTIPLY — multiply all wild multipliers on the matching streak; apply product to base win.
     *   SEQUENCE — ignore wildMultiplier; look up the combined multiplier from wildSequence[wildCount-1].
     *
     * If no wilds appear in the streak, lineMultiplier = 1.0 (no change to base win).
     */
    private static LineResult evalLineResult(int[][] screen, int reelCount,
                                             SymbolTable symbols, int[] line,
                                             boolean reversed, int minMatch) {
        int streak = 0, wildStreak = 0, totalWilds = 0;
        int paySymbol = -1;
        boolean allWild = true;

        double wildAcc = 0.0;
        boolean hasWild = false;
        WildMultiplierAggregation aggregationType = null;
        SymbolConfig firstWildCfg = null;

        for (int ri = 0; ri < reelCount; ri++) {
            int reel = reversed ? (reelCount - 1 - ri) : ri;
            int row  = line[reel];
            int sym  = screen[reel][row];

            if (symbols.isScatter(sym)) {
                if (ri == 0) return new LineResult(0.0, -1, 0);
                break;
            }

            boolean isWild = symbols.isWild(sym);
            if (!isWild && paySymbol < 0) paySymbol = sym;

            if (allWild || isWild || sym == paySymbol) {
                streak++;
                if (isWild) {
                    if (allWild) wildStreak++;
                    totalWilds++;
                    SymbolConfig wCfg = symbols.get(sym);
                    if (wCfg != null) {
                        if (!hasWild) {
                            aggregationType = wCfg.wildAggregation();
                            firstWildCfg = wCfg;
                            hasWild = true;
                            if (aggregationType == WildMultiplierAggregation.ADD) {
                                wildAcc = wCfg.wildMultiplier();
                            } else if (aggregationType == WildMultiplierAggregation.MULTIPLY) {
                                wildAcc = wCfg.wildMultiplier();
                            }
                        } else {
                            if (aggregationType == WildMultiplierAggregation.ADD) {
                                wildAcc += wCfg.wildMultiplier();
                            } else if (aggregationType == WildMultiplierAggregation.MULTIPLY) {
                                wildAcc *= wCfg.wildMultiplier();
                            }
                        }
                    }
                } else {
                    allWild = false;
                }
            } else {
                break;
            }
        }

        if (streak < minMatch) return new LineResult(0.0, -1, 0);

        double lineMultiplier;
        if (!hasWild) {
            lineMultiplier = 1.0;
        } else if (aggregationType == WildMultiplierAggregation.NONE) {
            lineMultiplier = 1.0;
        } else if (aggregationType == WildMultiplierAggregation.SEQUENCE) {
            List<Double> seq = firstWildCfg.wildSequence();
            int idx = totalWilds - 1;
            lineMultiplier = (seq != null && idx >= 0 && idx < seq.size()) ? seq.get(idx) : 1.0;
        } else {
            lineMultiplier = wildAcc > 0 ? wildAcc : 1.0;
        }

        double normalWin = 0.0;
        if (paySymbol >= 0) {
            SymbolConfig cfg = symbols.get(paySymbol);
            if (cfg != null) normalWin = payoutAt(cfg, streak, minMatch) * lineMultiplier;
        }

        double wildWin = 0.0;
        if (wildStreak >= minMatch && firstWildCfg != null) {
            wildWin = payoutAt(firstWildCfg, wildStreak, minMatch);
            if (wildWin == 0.0) {
                for (SymbolConfig sc : symbols.all()) {
                    if (sc.type() == SymbolType.NORMAL) {
                        double v = payoutAt(sc, wildStreak, minMatch);
                        if (v > wildWin) wildWin = v;
                    }
                }
            }
        }

        double win = Math.max(normalWin, wildWin);
        // effective symbol for tracking: if wild-only win, use wild symbol id
        int trackSym = (win == wildWin && wildWin > 0 && normalWin <= wildWin && firstWildCfg != null)
                ? firstWildCfg.symbolId() : paySymbol;
        int trackStreak = (win == wildWin && wildWin > 0 && wildStreak >= minMatch) ? wildStreak : streak;
        return new LineResult(win, win > 0 ? trackSym : -1, trackStreak);
    }

    private static double payoutAt(SymbolConfig cfg, int streak, int minMatch) {
        List<Double> pt = cfg.paytable();
        if (pt == null || pt.isEmpty()) return 0.0;
        int idx = streak - minMatch;
        if (idx < 0 || idx >= pt.size()) return 0.0;
        double v = pt.get(idx);
        return v > 0 ? v : 0.0;
    }
}
