package com.slotslab.agent;

import com.slotslab.agent.execution.ExecutionTrace;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.human.*;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.model.GeneratedReels;
import com.slotslab.agent.orchestrator.nodes.HumanApprovalNode;
import com.slotslab.reel.ReelSetsCollectionData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HumanApprovalNodeTest {

    private AgentContext makeContext(UUID executionId) {
        AgentRequest req = new AgentRequest(95.5, 0.15, 30.0, 5.0, "medium",
                new ReelSetsCollectionData(null, List.of()), null);
        return new AgentContext(executionId, req, new ExecutionTrace())
                .setReels(new GeneratedReels(req.reelConfig(), "[]"));
    }

    @Test
    void approvedResultContinues() {
        UUID id = UUID.randomUUID();
        DefaultHumanApprovalService svc = new DefaultHumanApprovalService();
        svc.submitApprovalForExecution(id, new ApprovalResult(ApprovalStatus.APPROVED, "Looks good"));

        HumanApprovalNode node = new HumanApprovalNode(svc, true);
        NodeResult result = node.execute(makeContext(id));

        assertTrue(result.isSuccess());
        assertEquals("APPROVED", result.getOutcome());
    }

    @Test
    void rejectedResultFails() {
        UUID id = UUID.randomUUID();
        DefaultHumanApprovalService svc = new DefaultHumanApprovalService();
        svc.submitApprovalForExecution(id, new ApprovalResult(ApprovalStatus.REJECTED, "Not good enough"));

        HumanApprovalNode node = new HumanApprovalNode(svc, true);
        NodeResult result = node.execute(makeContext(id));

        assertFalse(result.isSuccess());
        assertEquals("REJECTED", result.getError());
    }

    @Test
    void pendingResultSucceedsWithPendingOutcome() {
        UUID id = UUID.randomUUID();
        DefaultHumanApprovalService svc = new DefaultHumanApprovalService();

        HumanApprovalNode node = new HumanApprovalNode(svc, true);
        NodeResult result = node.execute(makeContext(id));

        assertTrue(result.isSuccess());
        assertEquals("PENDING", result.getOutcome());
    }

    @Test
    void disabledHumanApprovalAutoApproves() {
        DefaultHumanApprovalService svc = new DefaultHumanApprovalService();
        HumanApprovalNode node = new HumanApprovalNode(svc, false);

        NodeResult result = node.execute(makeContext(UUID.randomUUID()));

        assertTrue(result.isSuccess());
        assertEquals("APPROVED", result.getOutcome());
        assertEquals(Boolean.FALSE, result.getMetadata().get("humanInTheLoop"));
    }
}
