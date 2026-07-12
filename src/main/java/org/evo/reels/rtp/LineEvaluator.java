package org.evo.reels.rtp;

import java.util.List;

/**
 * Core line-based win evaluator.
 *
 * screen[reel][row] — primitive arrays for performance.
 * lines[lineIdx][reelIdx] = row position (0-based).
 */
final class LineEvaluator {

    private LineEvaluator() {}

    static double evalLtr(int[][] screen, int reelCount, SymbolTable symbols, int[][] lines, int minMatch) {
        double total = 0.0;
        for (int[] line : lines) total += evalLine(screen, reelCount, symbols, line, false, minMatch);
        return total;
    }

    static double evalRtl(int[][] screen, int reelCount, SymbolTable symbols, int[][] lines, int minMatch) {
        double total = 0.0;
        for (int[] line : lines) total += evalLine(screen, reelCount, symbols, line, true, minMatch);
        return total;
    }

    /**
     * Evaluates a single payline in one direction. Returns win multiplier (0 if no win).
     *
     * Wild multiplier aggregation rules:
     *   ADD      — sum all wild multipliers on the matching streak; apply sum as multiplier to base win.
     *   MULTIPLY — multiply all wild multipliers on the matching streak; apply product to base win.
     *   SEQUENCE — ignore wildMultiplier; look up the combined multiplier from wildSequence[wildCount-1].
     *
     * If no wilds appear in the streak, lineMultiplier = 1.0 (no change to base win).
     */
    private static double evalLine(int[][] screen, int reelCount,
                                   SymbolTable symbols, int[] line, boolean reversed, int minMatch) {
        int streak = 0, wildStreak = 0, totalWilds = 0;
        int paySymbol = -1;
        boolean allWild = true;

        // Accumulator for ADD / MULTIPLY; resolved for SEQUENCE after streak ends.
        double wildAcc = 0.0;          // sum (ADD) or product (MULTIPLY) of wilds seen
        boolean hasWild = false;
        WildMultiplierAggregation aggregationType = null;
        SymbolConfig firstWildCfg = null;

        for (int ri = 0; ri < reelCount; ri++) {
            int reel = reversed ? (reelCount - 1 - ri) : ri;
            int row  = line[reel];
            int sym  = screen[reel][row];

            if (symbols.isScatter(sym)) {
                if (ri == 0) return 0.0;
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
                            // Initialise accumulator for the chosen mode
                            if (aggregationType == WildMultiplierAggregation.ADD) {
                                wildAcc = wCfg.wildMultiplier();
                            } else if (aggregationType == WildMultiplierAggregation.MULTIPLY) {
                                wildAcc = wCfg.wildMultiplier();
                            }
                            // SEQUENCE: resolved after streak; wildAcc unused
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

        if (streak < minMatch) return 0.0;

        // Resolve line multiplier
        double lineMultiplier;
        if (!hasWild) {
            lineMultiplier = 1.0;
        } else if (aggregationType == WildMultiplierAggregation.NONE) {
            lineMultiplier = 1.0;
        } else if (aggregationType == WildMultiplierAggregation.SEQUENCE) {
            // Index by total wilds in the matching streak (not just leading wilds).
            List<Double> seq = firstWildCfg.wildSequence();
            int idx = totalWilds - 1;
            lineMultiplier = (seq != null && idx >= 0 && idx < seq.size()) ? seq.get(idx) : 1.0;
        } else {
            // ADD or MULTIPLY — wildAcc is already the final value
            lineMultiplier = wildAcc > 0 ? wildAcc : 1.0;
        }

        // Normal symbol payout
        double normalWin = 0.0;
        if (paySymbol >= 0) {
            SymbolConfig cfg = symbols.get(paySymbol);
            if (cfg != null) normalWin = payoutAt(cfg, streak, minMatch) * lineMultiplier;
        }

        // Wild-only payout (all matched positions were wilds)
        double wildWin = 0.0;
        if (wildStreak >= minMatch && firstWildCfg != null) {
            wildWin = payoutAt(firstWildCfg, wildStreak, minMatch);
            // If wild has no paytable, fall back to the best normal symbol payout for that streak length
            if (wildWin == 0.0) {
                for (SymbolConfig sc : symbols.all()) {
                    if (sc.type() == SymbolType.NORMAL) {
                        double v = payoutAt(sc, wildStreak, minMatch);
                        if (v > wildWin) wildWin = v;
                    }
                }
            }
        }

        return Math.max(normalWin, wildWin);
    }

    /**
     * Looks up payout for a given streak length.
     * Paytable index 0 = minMatch hits, index 1 = minMatch+1, etc.
     */
    private static double payoutAt(SymbolConfig cfg, int streak, int minMatch) {
        List<Double> pt = cfg.paytable();
        if (pt == null || pt.isEmpty()) return 0.0;
        int idx = streak - minMatch;
        if (idx < 0 || idx >= pt.size()) return 0.0;
        double v = pt.get(idx);
        return v > 0 ? v : 0.0;
    }
}
