package com.slotslab.agent.orchestrator.nodes;

import com.slotslab.agent.graph.AgentNode;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.tools.AgentReelGenerationTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

public class GenerateReelsNode implements AgentNode {

    public static final String ID = "generate-reels";
    private static final Logger log = LoggerFactory.getLogger(GenerateReelsNode.class);

    private final List<AgentReelGenerationTool> tools;

    public GenerateReelsNode(List<AgentReelGenerationTool> tools) {
        this.tools = tools;
    }

    @Override
    public String getId() { return ID; }

    @Override
    public NodeResult execute(AgentContext context) {
        context.incrementAttempts();

        String strategy = (String) context.getVariables()
                .getOrDefault(AnalyzeRequestNode.KEY_STRATEGY, "LTR");

        AgentReelGenerationTool tool = tools.stream()
                .filter(t -> t.supports(strategy))
                .findFirst()
                .orElse(null);

        if (tool == null) {
            return NodeResult.failure(
                "Strategy '" + strategy + "' is not supported. " +
                "Currently supported strategies: LTR.");
        }

        // Merge retry hints into a fresh params map so the original request is never mutated
        AgentRequest request = context.getRequest();
        Map<String, Object> hints = context.getRetryHints();
        AgentRequest effectiveRequest = hints.isEmpty() ? request : mergeHints(request, hints);

        Map<String, Object> ep = effectiveRequest.parameters();
        log.info("[generate-reels] attempt={} effective params: maxIterations={} symsPerReel={} " +
                 "screenWidth={} screenHeight={} minMatch={} targetVolatility={} seed={} " +
                 "symbols={} lines={}",
                context.getAttempts(),
                ep.getOrDefault("maxIterations", "?"),
                ep.getOrDefault("symsPerReel", "?"),
                ep.getOrDefault("screenWidth", "?"),
                ep.getOrDefault("screenHeight", "?"),
                ep.getOrDefault("minMatch", "?"),
                ep.getOrDefault("targetVolatility", "?"),
                ep.getOrDefault("seed", "?"),
                ep.containsKey("symbols") ? ((List<?>) ep.get("symbols")).size() + " symbols" : "none",
                ep.containsKey("lines")   ? ((List<?>) ep.get("lines")).size()   + " lines"   : "none");

        try {
            var reels = tool.generate(effectiveRequest, context);
            context.setReels(reels);
            // Log the simulation result so we can see convergence at a glance
            logSimResult(reels.rawJson(), context.getAttempts());
            return NodeResult.success("reels-generated")
                    .withMeta("attempt", context.getAttempts())
                    .withMeta("strategy", strategy)
                    .withMeta("reelSetsCount", reels.config().reelSets().size());
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            return NodeResult.failure("Generation failed: " + e.getMessage())
                    .withMeta("attempt", context.getAttempts());
        }
    }

    private AgentRequest mergeHints(AgentRequest request, Map<String, Object> hints) {
        Map<String, Object> params = new HashMap<>(
                request.parameters() != null ? request.parameters() : Map.of());
        params.putAll(hints);
        return new AgentRequest(
                request.targetRtp(), request.rtpDelta(),
                request.targetHitRate(), request.hitRateDelta(),
                request.targetVolatility(), request.maxPayout(),
                request.reelConfig(), params);
    }

    @SuppressWarnings("unchecked")
    private void logSimResult(String rawJson, int attempt) {
        if (rawJson == null) return;
        try {
            // Quick parse — avoid importing ObjectMapper; just find "rtp", "hitRate", "converged"
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> root = m.readValue(rawJson, Map.class);
            Boolean converged = (Boolean) root.get("converged");
            Object sim = root.get("simulation");
            if (sim instanceof Map<?,?> simMap) {
                log.info("[generate-reels] attempt={} RESULT: converged={} rtp={} hitRate={} " +
                         "volatilityLabel={} maxWin={}",
                        attempt, converged,
                        simMap.get("rtp"), simMap.get("hitRate"),
                        simMap.get("volatilityLabel"), simMap.get("maxWin"));
            }
        } catch (Exception ignored) {}
    }
}
