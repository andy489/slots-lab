package com.slotslab.agent.api;

import com.slotslab.agent.api.dto.AgentRunRequest;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AgentRequestValidator {

    private static final Set<String> VALID_STRATEGIES = Set.of(
            "LTR", "RTL", "BW", "SL", "ADJ", "WAYS", "MEGAWAYS", "SCATTERS", "CLUSTERS");
    private static final Set<String> LINE_STRATEGIES = Set.of("LTR", "RTL", "BW", "SL", "ADJ");
    private static final Set<String> VALID_TIERS = Set.of(
            "senior", "junior", "wild", "scatter", "multiwild");
    private static final Set<String> VALID_VOLATILITIES = Set.of(
            "LOW", "CASUAL", "HIGH", "VERY_HIGH", "EXTREME", "ULTRA_EXTREME");

    public List<String> validate(AgentRunRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.rtpDelta() >= req.targetRtp()) {
            errors.add("rtpDelta must be < targetRtp");
        }
        if (req.hitRateDelta() >= req.targetHitRate()) {
            errors.add("hitRateDelta must be < targetHitRate");
        }
        if (req.maxPayout() < 0)
            errors.add("maxPayout must be ≥ 0 (0 = uncapped)");
        String volatility = req.targetVolatility();
        if (volatility == null || volatility.isBlank()) {
            errors.add("targetVolatility is required");
        } else if (!VALID_VOLATILITIES.contains(volatility.toUpperCase())) {
            errors.add("targetVolatility '" + volatility + "' is not valid; expected one of " + VALID_VOLATILITIES);
        }

        Map<String, Object> params = req.parameters();
        if (params == null) return errors;

        String strategy = asString(params.get("strategy"));
        if (strategy == null || strategy.isBlank()) {
            errors.add("parameters.strategy is required");
        } else if (!VALID_STRATEGIES.contains(strategy)) {
            errors.add("parameters.strategy '" + strategy + "' is not valid; expected one of " + VALID_STRATEGIES);
        }

        int screenWidth  = asInt(params.get("screenWidth"),  -1);
        int screenHeight = asInt(params.get("screenHeight"), -1);
        int minMatch     = asInt(params.get("minMatch"),     -1);
        int symsPerReel      = asInt(params.get("symsPerReel"),      -1);
        int symsPerReelDelta = asInt(params.get("symsPerReelDelta"),   0);
        int maxIterations    = asInt(params.get("maxIterations"),       5);

        if (screenWidth < 1 || screenWidth > 20)
            errors.add("parameters.screenWidth must be 1–20");
        if (screenHeight < 1 || screenHeight > 10)
            errors.add("parameters.screenHeight must be 1–10");
        if (minMatch < 1 || minMatch > 20)
            errors.add("parameters.minMatch must be 1–20");
        if (symsPerReel < 16 || symsPerReel > 512)
            errors.add("parameters.symsPerReel must be 16–512");
        if (symsPerReelDelta < 0 || (symsPerReel > 0 && symsPerReelDelta >= symsPerReel))
            errors.add("parameters.symsPerReelDelta must be ≥ 0 and < symsPerReel");
        if (screenWidth > 0 && minMatch > screenWidth)
            errors.add("parameters.minMatch (" + minMatch + ") cannot exceed screenWidth (" + screenWidth + ")");
        if (maxIterations < 1 || maxIterations > 50)
            errors.add("parameters.maxIterations must be 1–50");

        Object symsObj = params.get("symbols");
        if (!(symsObj instanceof List<?> symsList) || symsList.size() < 2) {
            errors.add("parameters.symbols must contain at least 2 entries");
        } else {
            Set<Integer> seenIds = new LinkedHashSet<>();
            for (int i = 0; i < symsList.size(); i++) {
                Object s = symsList.get(i);
                if (!(s instanceof Map<?, ?> sym)) {
                    errors.add("parameters.symbols[" + i + "] must be an object");
                    continue;
                }
                int id = asInt(sym.get("symbolId"), -1);
                if (id < 1 || id > 99)
                    errors.add("parameters.symbols[" + i + "].symbolId must be 1–99");
                else if (!seenIds.add(id))
                    errors.add("parameters.symbols[" + i + "].symbolId=" + id + " is duplicated");

                String tier = asString(sym.get("tier"));
                if (tier == null || !VALID_TIERS.contains(tier))
                    errors.add("parameters.symbols[" + i + "].tier must be one of " + VALID_TIERS);
            }
        }

        if (strategy != null && LINE_STRATEGIES.contains(strategy) && req.reelConfig() == null) {
            Object linesObj = params.get("lines");
            if (!(linesObj instanceof List<?> lines) || lines.isEmpty()) {
                errors.add("parameters.lines is required for " + strategy + " strategy");
            } else if (screenWidth > 0 && screenHeight > 0) {
                for (int i = 0; i < lines.size(); i++) {
                    Object row = lines.get(i);
                    if (!(row instanceof List<?> rowList)) {
                        errors.add("parameters.lines[" + i + "] must be an array");
                        continue;
                    }
                    if (rowList.size() != screenWidth)
                        errors.add("parameters.lines[" + i + "] must have " + screenWidth +
                                " entries (one per reel), got " + rowList.size());
                    for (int j = 0; j < rowList.size(); j++) {
                        int rowIdx = asInt(rowList.get(j), -1);
                        if (rowIdx < 0 || rowIdx >= screenHeight)
                            errors.add("parameters.lines[" + i + "][" + j + "] row index must be 0–" + (screenHeight - 1));
                    }
                }
            }
        }

        return errors;
    }

    private static String asString(Object v) {
        return v instanceof String s ? s : null;
    }

    private static int asInt(Object v, int fallback) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }
}
