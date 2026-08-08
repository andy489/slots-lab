package com.slotslab.agent.skills.ltr;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaytableGenerator {

    public Map<Integer, Map<Integer, Double>> generate(
            List<SymbolDef> symbols, int minMatch, int screenWidth, String volatility) {

        Map<Integer, Map<Integer, Double>> paytable = new LinkedHashMap<>();
        for (SymbolDef sym : symbols) {
            Map<Integer, Double> symPay = new LinkedHashMap<>();
            double base = baseMultiplier(sym, volatility);
            double growth = growthFactor(volatility);
            double current = base;
            for (int match = minMatch; match <= screenWidth; match++) {
                symPay.put(match, round(current));
                current *= growth;
            }
            paytable.put(sym.symbolId(), symPay);
        }
        return paytable;
    }

    private double baseMultiplier(SymbolDef sym, String volatility) {
        if (sym.isScatter()) return 2.00;
        String vol = volatility == null ? "CASUAL" : volatility.toUpperCase();
        if (sym.isMultiWild()) return seniorBase(vol) * 2.0;
        if (sym.isWild())      return seniorBase(vol) * 1.5;
        if (sym.isSenior())    return seniorBase(vol);
        return switch (vol) { // junior
            case "LOW"          -> 1.00;
            case "CASUAL"       -> 1.80;
            case "HIGH"         -> 3.00;
            case "VERY_HIGH"    -> 5.00;
            case "EXTREME"      -> 9.00;
            case "ULTRA_EXTREME"-> 15.00;
            default             -> 1.80;
        };
    }

    private double seniorBase(String vol) {
        return switch (vol) {
            case "LOW"          -> 2.50;
            case "CASUAL"       -> 5.00;
            case "HIGH"         -> 10.00;
            case "VERY_HIGH"    -> 20.00;
            case "EXTREME"      -> 40.00;
            case "ULTRA_EXTREME"-> 80.00;
            default             -> 5.00;
        };
    }

    private double growthFactor(String volatility) {
        if (volatility == null) return 3.0;
        return switch (volatility.toUpperCase()) {
            case "LOW"          -> 2.5;
            case "CASUAL"       -> 3.0;
            case "HIGH"         -> 4.0;
            case "VERY_HIGH"    -> 6.0;
            case "EXTREME"      -> 10.0;
            case "ULTRA_EXTREME"-> 16.0;
            default             -> 3.0;
        };
    }

    private double round(double v) {
        double rounded = Math.round(v / 0.10) * 0.10;
        return Math.max(0.10, Math.round(rounded * 100.0) / 100.0);
    }
}
