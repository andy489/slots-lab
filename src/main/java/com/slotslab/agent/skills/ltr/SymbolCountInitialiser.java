package com.slotslab.agent.skills.ltr;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SymbolCountInitialiser {

    public int[] initialise(List<SymbolDef> symbols, String volatility, int symsPerReel) {
        double seniorFactor = seniorFactor(volatility);

        // Rank within tier by symbolId ascending; lower id = more tiles (higher hit rate)
        List<Integer> juniorIds = new ArrayList<>();
        List<Integer> seniorIds = new ArrayList<>();
        for (SymbolDef sym : symbols) {
            if (sym.isJunior()) juniorIds.add(sym.symbolId());
            else if (sym.isSenior()) seniorIds.add(sym.symbolId());
        }
        Collections.sort(juniorIds);
        Collections.sort(seniorIds);

        double[] weights = new double[symbols.size()];
        double totalWeight = 0;
        for (int i = 0; i < symbols.size(); i++) {
            weights[i] = tierWeight(symbols.get(i), seniorFactor, juniorIds, seniorIds);
            totalWeight += weights[i];
        }

        int[] counts = new int[symbols.size()];
        int assigned = 0;
        for (int i = 0; i < symbols.size(); i++) {
            counts[i] = Math.max(1, (int) Math.floor(symsPerReel * weights[i] / totalWeight));
            assigned += counts[i];
        }

        // distribute remainder to lowest-id junior symbol
        int remainder = symsPerReel - assigned;
        for (int i = 0; i < symbols.size() && remainder > 0; i++) {
            if (symbols.get(i).isJunior()) {
                counts[i]++;
                remainder--;
            }
        }
        // fallback: any symbol
        for (int i = 0; i < symbols.size() && remainder > 0; i++) {
            counts[i]++;
            remainder--;
        }
        return counts;
    }

    private double tierWeight(SymbolDef sym, double seniorFactor,
                               List<Integer> juniorIds, List<Integer> seniorIds) {
        if (sym.isMultiWild()) return 0.12;
        if (sym.isScatter())   return 0.15;
        if (sym.isWild())      return 0.20;
        // Inverse rank scale: rank 0 (lowest id) → more tiles, rank n-1 → fewer tiles
        if (sym.isSenior()) {
            return seniorFactor * inverseRankScale(seniorIds.indexOf(sym.symbolId()), seniorIds.size());
        }
        return 1.0 * inverseRankScale(juniorIds.indexOf(sym.symbolId()), juniorIds.size()); // junior base = 1.0
    }

    /**
     * Inverse rank: rank 0 (lowest id) → hi weight (most tiles), rank n-1 → lo weight.
     */
    private double inverseRankScale(int rankIdx, int tierSize) {
        if (tierSize <= 1) return 1.0;
        double lo = 0.70, hi = 1.30;
        // rank 0 → hi, rank n-1 → lo
        return hi - (hi - lo) * rankIdx / (tierSize - 1);
    }

    private double seniorFactor(String volatility) {
        if (volatility == null) return 0.50;
        return switch (volatility.toUpperCase()) {
            case "LOW"          -> 0.65;
            case "CASUAL"       -> 0.50;
            case "HIGH"         -> 0.35;
            case "VERY_HIGH"    -> 0.22;
            case "EXTREME"      -> 0.13;
            case "ULTRA_EXTREME"-> 0.07;
            default             -> 0.50;
        };
    }
}
