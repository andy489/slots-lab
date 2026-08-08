package com.slotslab.agent;

import com.slotslab.agent.execution.ExecutionTrace;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.model.GeneratedReels;
import com.slotslab.agent.orchestrator.nodes.AnalyzeRequestNode;
import com.slotslab.agent.orchestrator.nodes.GenerateReelsNode;
import com.slotslab.agent.orchestrator.nodes.ValidateReelsNode;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.tools.AgentReelGenerationTool;
import com.slotslab.agent.validation.BasicReelValidator;
import com.slotslab.reel.ReelSet;
import com.slotslab.reel.ReelSetsCollectionData;
import com.slotslab.reel.Strategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MaxAttemptsTest {

    private AgentContext makeContext() {
        ReelSetsCollectionData config = new ReelSetsCollectionData(Strategy.FLAT,
                List.of(new ReelSet(List.of(List.of(1, 2, 3)), null)));
        AgentRequest req = new AgentRequest(95.5, 0.15, 30.0, 5.0, "LTR", config,
                Map.of("strategy", "LTR"));
        AgentContext ctx = new AgentContext(UUID.randomUUID(), req, new ExecutionTrace());
        ctx.getVariables().put(AnalyzeRequestNode.KEY_STRATEGY, "LTR");
        return ctx;
    }

    @Test
    void agentStopsAfterMaxAttempts() {
        int maxAttempts = 3;
        AgentContext context = makeContext();

        // Tool always returns null JSON to force INVALID
        AgentReelGenerationTool badTool = new AgentReelGenerationTool() {
            @Override public boolean supports(String strategy) { return true; }
            @Override public GeneratedReels generate(AgentRequest request) {
                return new GeneratedReels(request.reelConfig(), null);
            }
        };
        GenerateReelsNode generateNode = new GenerateReelsNode(List.of(badTool));
        ValidateReelsNode validateNode = new ValidateReelsNode(new BasicReelValidator());

        for (int i = 0; i < maxAttempts; i++) {
            NodeResult genResult = generateNode.execute(context);
            assertTrue(genResult.isSuccess());
            NodeResult valResult = validateNode.execute(context);
            assertFalse(valResult.isSuccess());
        }

        // After maxAttempts, the graph transition condition (attempts < maxAttempts) is false
        assertFalse(context.getAttempts() < maxAttempts,
                "Agent should stop looping once maxAttempts is reached");
        assertEquals(maxAttempts, context.getAttempts());
    }
}
