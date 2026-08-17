package com.slotslab.agent.orchestrator.nodes;

import com.slotslab.agent.graph.AgentNode;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.reel.ReelSet;
import com.slotslab.reel.ReelSetsCollectionData;
import com.slotslab.reel.Strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnalyzeRequestNode implements AgentNode {

    public static final String ID = "analyze-request";
    public static final String KEY_RESOLVED_CONFIG = "resolvedConfig";
    public static final String KEY_STRATEGY = "strategy";

    @Override
    public String getId() { return ID; }

    @Override
    public NodeResult execute(AgentContext context) {
        var req = context.getRequest();
        if (req == null) return NodeResult.failure("AgentRequest is null");

        // Use explicit reelConfig if provided; otherwise build from parameters
        ReelSetsCollectionData config = req.reelConfig();
        if (config == null) {
            config = buildConfigFromParameters(req.parameters());
            if (config == null) {
                return NodeResult.failure(
                    "reelConfig is required, or supply strategy/screenWidth/symbols in parameters");
            }
        }

        context.getVariables().put(KEY_RESOLVED_CONFIG, config);
        String strategy = req.parameters() != null
                ? (String) req.parameters().getOrDefault("strategy", "LTR")
                : "LTR";
        context.getVariables().put(KEY_STRATEGY, strategy.toUpperCase());
        return NodeResult.success("request-valid")
                .withMeta("targetRtp", req.targetRtp())
                .withMeta("rtpDelta", req.rtpDelta())
                .withMeta("targetVolatility", req.targetVolatility())
                .withMeta("strategy", strategy);
    }

    @SuppressWarnings("unchecked")
    private ReelSetsCollectionData buildConfigFromParameters(Map<String, Object> params) {
        if (params == null) return null;

        // Generator strategy (SHUFFLE/FLAT) — distinct from the payout strategy (LTR/RTL/…)
        // resolved into KEY_STRATEGY. Falls back to FLAT, the only sensible uniform default.
        String stratStr = (String) params.getOrDefault("generatorStrategy", "FLAT");
        Strategy strategy;
        try { strategy = Strategy.valueOf(stratStr.toUpperCase()); }
        catch (IllegalArgumentException e) { strategy = Strategy.FLAT; }

        // dimensions
        int screenWidth  = toInt(params.get("screenWidth"),  5);
        int screenHeight = toInt(params.get("screenHeight"), 3);

        // symbols list: [{symbolId, tier, hint}, ...]
        List<Map<String, Object>> symbols = (List<Map<String, Object>>) params.get("symbols");
        if (symbols == null || symbols.isEmpty()) return null;

        // Build one ReelSet: screenWidth reels, each reel has one count per symbol
        // Default count per symbol = screenHeight (uniform starting distribution)
        List<List<Integer>> tilesCounts = new ArrayList<>();
        for (int r = 0; r < screenWidth; r++) {
            List<Integer> reel = new ArrayList<>();
            for (Map<String, Object> sym : symbols) {
                reel.add(screenHeight);
            }
            tilesCounts.add(reel);
        }

        ReelSet reelSet = new ReelSet(null, tilesCounts, Collections.emptyList());
        return new ReelSetsCollectionData(strategy, List.of(reelSet));
    }

    private int toInt(Object v, int defaultVal) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) { try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
        return defaultVal;
    }
}
