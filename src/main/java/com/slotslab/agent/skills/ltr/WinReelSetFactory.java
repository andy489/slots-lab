package com.slotslab.agent.skills.ltr;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces one winning reel set per non-special symbol by injecting a
 * left-to-right decaying win-count vector into the symbol's column.
 */
@Component
public class WinReelSetFactory {

    public record WinReelEntry(int[][] reelSet, SymbolDef symbol) {}

    private static final int DEFAULT_RESIDUAL_FILL = 8;

    public List<WinReelEntry> create(List<int[][]> noWinReelSets, List<SymbolDef> symbols,
                                int[] baseCounts, int screenWidth, String volatility) {
        double decay = decayFactor(volatility);
        List<WinReelEntry> winSets = new ArrayList<>();

        for (int symIdx = 0; symIdx < symbols.size(); symIdx++) {
            SymbolDef sym = symbols.get(symIdx);
            if (sym.isSpecial()) continue;

            // clone the base no-win reel set (cycled)
            int[][] base = noWinReelSets.get(symIdx % noWinReelSets.size());
            int numSymbols = baseCounts.length;
            int[][] reelSet = new int[screenWidth][numSymbols];
            for (int r = 0; r < screenWidth; r++) {
                System.arraycopy(base[r], 0, reelSet[r], 0, numSymbols);
            }

            // build win vector for this symbol
            double peak = sym.isSenior()
                    ? baseCounts[symIdx] * 1.5
                    : baseCounts[symIdx] * 1.2;
            int[] winVec = new int[screenWidth];
            winVec[0] = Math.max(1, (int) Math.round(peak));
            for (int r = 1; r < screenWidth; r++) {
                winVec[r] = Math.max(1, (int) Math.round(winVec[r - 1] * decay));
            }

            // inject win vector into symbol's column
            for (int r = 0; r < screenWidth; r++) {
                reelSet[r][symIdx] = winVec[r];
            }

            // fill residual zeroes left by other symbols
            for (int r = 0; r < screenWidth; r++) {
                for (int s = 0; s < numSymbols; s++) {
                    if (s != symIdx && reelSet[r][s] == 0) {
                        reelSet[r][s] = DEFAULT_RESIDUAL_FILL;
                    }
                }
            }
            winSets.add(new WinReelEntry(reelSet, sym));
        }
        return winSets;
    }

    private double decayFactor(String volatility) {
        if (volatility == null) return 0.88;
        return switch (volatility.toUpperCase()) {
            case "LOW"          -> 0.94;
            case "CASUAL"       -> 0.88;
            case "HIGH"         -> 0.78;
            case "VERY_HIGH"    -> 0.68;
            case "EXTREME"      -> 0.55;
            case "ULTRA_EXTREME"-> 0.45;
            default             -> 0.88;
        };
    }
}
