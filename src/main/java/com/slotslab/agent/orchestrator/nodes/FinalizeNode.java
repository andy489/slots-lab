package com.slotslab.agent.orchestrator.nodes;

import com.slotslab.agent.graph.AgentNode;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.model.AgentContext;

public class FinalizeNode implements AgentNode {

    public static final String ID = "finalize";

    @Override
    public String getId() { return ID; }

    @Override
    public NodeResult execute(AgentContext context) {
        var reels = context.getReels();
        if (reels == null) {
            return NodeResult.failure("No reels to finalize");
        }
        return NodeResult.success("DONE")
                .withMeta("rawJson", reels.rawJson());
    }
}
