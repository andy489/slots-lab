package com.slotslab.agent.orchestrator.nodes;

import com.slotslab.agent.graph.AgentNode;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.tools.AgentReelGenerationTool;

import java.util.List;
import java.util.concurrent.CancellationException;

public class GenerateReelsNode implements AgentNode {

    public static final String ID = "generate-reels";

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

        try {
            var reels = tool.generate(context.getRequest(), context);
            context.setReels(reels);
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
}
