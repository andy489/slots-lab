package com.slotslab.simulation.eval;

import com.slotslab.simulation.config.SymbolConfig;
import com.slotslab.simulation.config.SymbolTable;
import com.slotslab.simulation.config.SymbolType;
import com.slotslab.simulation.config.WildMultiplierAggregation;
import com.slotslab.simulation.stats.ComboKey;

import java.util.List;
import java.util.Map;

public final class LineEvaluator {

    private LineEvaluator() {}

    private record LineResult(double win, int paySymbol, int streak) {}

    // ── untracked (fast path) ────────────────────────────────────────────────

    public static double evalLtr(int[][] screen, int reelCount, SymbolTable symbols, int[][] lines, int minMatch) {
        double total = 0.0;
        for (int[] line : lines) total += evalLine(screen, reelCount, symbols, line, 0, false, minMatch).win();
        return total;
    }

    public static double evalRtl(int[][] screen, int reelCount, SymbolTable symbols, int[][] lines, int minMatch) {
        double total = 0.0;
        for (int[] line : lines) total += evalLine(screen, reelCount, symbols, line, 0, true, minMatch).win();
        return total;
    }

    public static double evalAdj(int[][] screen, int reelCount, SymbolTable symbols, int[][] lines, int minMatch) {
        double total = 0.0;
        for (int[] line : lines) total += evalAdjLine(screen, reelCount, symbols, line, minMatch).win();
        return total;
    }

    public static double evalSl(int[][] screen, int reelCount, SymbolTable symbols, int[][] lines, int minMatch) {
        double total = 0.0;
        for (int[] line : lines) total += evalSuperLine(screen, reelCount, symbols, line, minMatch).win();
        return total;
    }

    // ── tracked variants ─────────────────────────────────────────────────────

    public static double evalLtrTracked(int[][] screen, int reelCount, SymbolTable symbols,
                                 int[][] lines, int minMatch,
                                 Map<ComboKey, long[]> hitMap, Map<ComboKey, double[]> payMap) {
        double total = 0.0;
        for (int[] line : lines) {
            LineResult r = evalLine(screen, reelCount, symbols, line, 0, false, minMatch);
            total += r.win();
            if (r.win() > 0 && r.paySymbol() >= 0)
                record(r.paySymbol(), r.streak(), r.win(), hitMap, payMap);
        }
        return total;
    }

    public static double evalRtlTracked(int[][] screen, int reelCount, SymbolTable symbols,
                                 int[][] lines, int minMatch,
                                 Map<ComboKey, long[]> hitMap, Map<ComboKey, double[]> payMap) {
        double total = 0.0;
        for (int[] line : lines) {
            LineResult r = evalLine(screen, reelCount, symbols, line, 0, true, minMatch);
            total += r.win();
            if (r.win() > 0 && r.paySymbol() >= 0)
                record(r.paySymbol(), r.streak(), r.win(), hitMap, payMap);
        }
        return total;
    }

    public static double evalAdjTracked(int[][] screen, int reelCount, SymbolTable symbols,
                                 int[][] lines, int minMatch,
                                 Map<ComboKey, long[]> hitMap, Map<ComboKey, double[]> payMap) {
        double total = 0.0;
        for (int[] line : lines) {
            LineResult r = evalAdjLine(screen, reelCount, symbols, line, minMatch);
            total += r.win();
            if (r.win() > 0 && r.paySymbol() >= 0)
                record(r.paySymbol(), r.streak(), r.win(), hitMap, payMap);
        }
        return total;
    }

    public static double evalSlTracked(int[][] screen, int reelCount, SymbolTable symbols,
                                 int[][] lines, int minMatch,
                                 Map<ComboKey, long[]> hitMap, Map<ComboKey, double[]> payMap) {
        double total = 0.0;
        for (int[] line : lines) {
            LineResult r = evalSuperLine(screen, reelCount, symbols, line, minMatch);
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

    private static LineResult evalAdjLine(int[][] screen, int reelCount, SymbolTable symbols,
                                          int[] line, int minMatch) {
        LineResult best = new LineResult(0.0, -1, 0);
        int maxStart = reelCount - minMatch;
        for (int startReel = 0; startReel <= maxStart; startReel++) {
            LineResult r = evalLine(screen, reelCount, symbols, line, startReel, false, minMatch);
            if (r.win() > best.win()) best = r;
        }
        return best;
    }

    // ── super-lines evaluator (gaps allowed within a payline) ────────────────

    private static LineResult evalSuperLine(int[][] screen, int reelCount, SymbolTable symbols,
                                            int[] line, int minMatch) {
        // First pass: collect all symbols on this payline up to (but not including) a scatter.
        // Also accumulate wild multiplier data (shared across every candidate pay-symbol).
        int totalWilds = 0;
        double wildAcc = 0.0;
        boolean hasWild = false;
        WildMultiplierAggregation aggregationType = null;
        SymbolConfig firstWildCfg = null;

        // Count occurrences of each normal symbol on the line.
        java.util.Map<Integer, Integer> symCounts = new java.util.LinkedHashMap<>();

        for (int reel = 0; reel < reelCount; reel++) {
            int row = line[reel];
            int sym = screen[reel][row];

            if (symbols.isScatter(sym)) break;

            boolean isWild = symbols.isWild(sym);

            if (isWild) {
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
                // Normal symbol — count it (wilds are counted separately and added per-symbol below)
                symCounts.merge(sym, 1, Integer::sum);
            }
        }

        // Compute shared line multiplier (same regardless of which symbol wins)
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

        // Second pass: for every normal symbol found, matchCount = ownCount + totalWilds.
        // Pick the highest win across all candidates.
        LineResult best = new LineResult(0.0, -1, 0);

        for (java.util.Map.Entry<Integer, Integer> entry : symCounts.entrySet()) {
            int sym = entry.getKey();
            int matchCount = entry.getValue() + totalWilds;
            if (matchCount < minMatch) continue;
            SymbolConfig cfg = symbols.get(sym);
            if (cfg == null) continue;
            double win = payoutAt(cfg, matchCount, minMatch) * lineMultiplier;
            if (win > best.win()) best = new LineResult(win, sym, matchCount);
        }

        // All-wild case: no normal symbols seen, wilds alone form the line
        if (symCounts.isEmpty() && totalWilds >= minMatch && firstWildCfg != null) {
            double wildWin = payoutAt(firstWildCfg, totalWilds, minMatch);
            if (wildWin == 0.0) {
                for (SymbolConfig sc : symbols.all()) {
                    if (sc.type() == SymbolType.NORMAL) {
                        double v = payoutAt(sc, totalWilds, minMatch);
                        if (v > wildWin) wildWin = v;
                    }
                }
            }
            if (wildWin > best.win()) best = new LineResult(wildWin, firstWildCfg.symbolId(), totalWilds);
        }

        return best;
    }

    /**
     * Evaluates a single pay-line starting from {@code startReel}.
     * Pass {@code startReel=0} for LTR/RTL full-line evaluation.
     * {@code reversed=true} traverses reels right-to-left (RTL).
     */
    static LineResult evalLine(int[][] screen, int reelCount, SymbolTable symbols,
                               int[] line, int startReel, boolean reversed, int minMatch) {
        int streak = 0, wildStreak = 0, totalWilds = 0;
        int paySymbol = -1;
        boolean allWild = true;

        double wildAcc = 0.0;
        boolean hasWild = false;
        WildMultiplierAggregation aggregationType = null;
        SymbolConfig firstWildCfg = null;

        for (int ri = startReel; ri < reelCount; ri++) {
            int reel = reversed ? (reelCount - 1 - (ri - startReel)) : ri;
            int row  = line[reel];
            int sym  = screen[reel][row];

            if (symbols.isScatter(sym)) {
                if (ri == startReel) return new LineResult(0.0, -1, 0);
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
            if (wildWin == 0.0 && paySymbol < 0) {
                for (SymbolConfig sc : symbols.all()) {
                    if (sc.type() == SymbolType.NORMAL) {
                        double v = payoutAt(sc, wildStreak, minMatch);
                        if (v > wildWin) wildWin = v;
                    }
                }
            }
        }

        double win = Math.max(normalWin, wildWin);
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
