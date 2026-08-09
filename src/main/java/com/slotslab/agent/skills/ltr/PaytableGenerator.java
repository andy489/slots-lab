package com.slotslab.agent.skills.ltr;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaytableGenerator {

    public Map<Integer, Map<Integer, Double>> generate(
            List<SymbolDef> symbols, int minMatch, int screenWidth, String volatility,
            double targetHitRate) {
        return generate(symbols, minMatch, screenWidth, volatility, targetHitRate, 0.0);
    }

    public Map<Integer, Map<Integer, Double>> generate(
            List<SymbolDef> symbols, int minMatch, int screenWidth, String volatility,
            double targetHitRate, double maxPayout) {

        // Rank within tier by symbolId (ascending = lower rank = lower pay)
        List<Integer> juniorIds = new ArrayList<>();
        List<Integer> seniorIds = new ArrayList<>();
        for (SymbolDef sym : symbols) {
            if (sym.isJunior()) juniorIds.add(sym.symbolId());
            else if (sym.isSenior()) seniorIds.add(sym.symbolId());
        }
        Collections.sort(juniorIds);
        Collections.sort(seniorIds);

        Map<Integer, Map<Integer, Double>> paytable = new LinkedHashMap<>();
        for (SymbolDef sym : symbols) {
            Map<Integer, Double> symPay = new LinkedHashMap<>();
            double base   = baseMultiplier(sym, volatility, targetHitRate, juniorIds, seniorIds);
            double growth = growthFactor(volatility, sym.isSenior() || sym.isWild() || sym.isMultiWild());
            double current = base;
            for (int match = minMatch; match <= screenWidth; match++) {
                symPay.put(match, round(current));
                current *= growth;
            }
            paytable.put(sym.symbolId(), symPay);
        }
        if (maxPayout > 0) capPaytable(paytable, maxPayout);
        return paytable;
    }

    /** Clamp every multiplier so no single value exceeds maxPayout. */
    public void capPaytable(Map<Integer, Map<Integer, Double>> paytable, double maxPayout) {
        if (maxPayout <= 0) return;
        paytable.forEach((id, pays) ->
            pays.replaceAll((match, val) -> Math.min(val, maxPayout)));
    }

    private double baseMultiplier(SymbolDef sym, String volatility, double targetHitRate,
                                   List<Integer> juniorIds, List<Integer> seniorIds) {
        if (sym.isScatter()) return 2.00;
        String vol = volatility == null ? "CASUAL" : volatility.toUpperCase();
        if (sym.isMultiWild()) return seniorBase(vol) * 2.0;
        if (sym.isWild())      return seniorBase(vol) * 1.5;

        if (sym.isSenior()) {
            double base = seniorBase(vol);
            return base * rankScale(seniorIds.indexOf(sym.symbolId()), seniorIds.size());
        }
        // junior: high hit rate → sub-1x base so juniors contribute less to RTP
        // while still being chosen frequently (they appear more via small stacks + weights).
        // HR=5%  → full base; HR=50% → 0.1x (minimum fraction of a bet).
        double hrFraction = (Math.max(5.0, Math.min(50.0, targetHitRate)) - 5.0) / 45.0;
        double fullBase = switch (vol) {
            case "LOW"           -> 0.80;
            case "CASUAL"        -> 1.20;
            case "HIGH"          -> 2.00;
            case "VERY_HIGH"     -> 3.50;
            case "EXTREME"       -> 6.00;
            case "ULTRA_EXTREME" -> 10.00;
            default              -> 1.20;
        };
        // Lerp from fullBase (low HR) down to 0.10 (high HR)
        double base = fullBase * (1.0 - hrFraction) + 0.10 * hrFraction;
        return base * rankScale(juniorIds.indexOf(sym.symbolId()), juniorIds.size());
    }

    /**
     * Maps rank index (0=lowest id) to a pay scale factor.
     * rank 0 → 0.70, rank n-1 → 1.30, linear between.
     */
    private double rankScale(int rankIdx, int tierSize) {
        if (tierSize <= 1) return 1.0;
        double lo = 0.70, hi = 1.30;
        return lo + (hi - lo) * rankIdx / (tierSize - 1);
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

    private double growthFactor(String volatility, boolean senior) {
        if (volatility == null) return senior ? 3.5 : 3.0;
        double base = switch (volatility.toUpperCase()) {
            case "LOW"           -> 2.5;
            case "CASUAL"        -> 3.0;
            case "HIGH"          -> 4.0;
            case "VERY_HIGH"     -> 6.0;
            case "EXTREME"       -> 10.0;
            case "ULTRA_EXTREME" -> 16.0;
            default              -> 3.0;
        };
        // Seniors grow faster per reel: multiply by 1.25 at higher volatilities,
        // 1.15 at lower ones — this widens the senior-junior gap with each additional reel.
        if (!senior) return base;
        double seniorBoost = switch (volatility.toUpperCase()) {
            case "LOW"           -> 1.10;
            case "CASUAL"        -> 1.15;
            case "HIGH"          -> 1.20;
            case "VERY_HIGH"     -> 1.25;
            case "EXTREME"       -> 1.30;
            case "ULTRA_EXTREME" -> 1.35;
            default              -> 1.15;
        };
        return base * seniorBoost;
    }

    private double round(double v) {
        double rounded = Math.round(v / 0.10) * 0.10;
        return Math.max(0.10, Math.round(rounded * 100.0) / 100.0);
    }
}
