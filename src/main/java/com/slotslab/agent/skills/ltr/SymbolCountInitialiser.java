package com.slotslab.agent.skills.ltr;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SymbolCountInitialiser {

    public int[] initialise(List<SymbolDef> symbols, String volatility, int symsPerReel) {
        double seniorFactor = seniorFactor(volatility);
        double[] weights = new double[symbols.size()];
        double totalWeight = 0;
        for (int i = 0; i < symbols.size(); i++) {
            weights[i] = tierWeight(symbols.get(i), seniorFactor);
            totalWeight += weights[i];
        }

        int[] counts = new int[symbols.size()];
        int assigned = 0;
        for (int i = 0; i < symbols.size(); i++) {
            counts[i] = Math.max(1, (int) Math.floor(symsPerReel * weights[i] / totalWeight));
            assigned += counts[i];
        }

        // distribute remainder to junior symbols
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

    private double tierWeight(SymbolDef sym, double seniorFactor) {
        if (sym.isMultiWild()) return 0.12;
        if (sym.isScatter())   return 0.15;
        if (sym.isWild())      return 0.20;
        if (sym.isSenior())    return seniorFactor;
        return 1.0; // junior
    }

    private double seniorFactor(String volatility) {
        if (volatility == null) return 0.65;
        return switch (volatility.toUpperCase()) {
            case "LOW"          -> 0.80;
            case "CASUAL"       -> 0.65;
            case "HIGH"         -> 0.45;
            case "VERY_HIGH"    -> 0.30;
            case "EXTREME"      -> 0.18;
            case "ULTRA_EXTREME"-> 0.10;
            default             -> 0.65;
        };
    }
}
