package com.slotslab.agent.orchestrator.nodes;

import com.slotslab.agent.graph.AgentNode;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.human.ApprovalRequest;
import com.slotslab.agent.human.ApprovalResult;
import com.slotslab.agent.human.ApprovalStatus;
import com.slotslab.agent.human.HumanApprovalService;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.GeneratedReels;

public class HumanApprovalNode implements AgentNode {

    public static final String ID = "human-approval";

    private final HumanApprovalService approvalService;
    private final boolean enabled;

    public HumanApprovalNode(HumanApprovalService approvalService, boolean enabled) {
        this.approvalService = approvalService;
        this.enabled = enabled;
    }

    @Override
    public String getId() { return ID; }

    @Override
    public NodeResult execute(AgentContext context) {
        if (!enabled) {
            return NodeResult.success("APPROVED").withMeta("humanInTheLoop", false);
        }
        GeneratedReels reels = context.getReels();
        ApprovalRequest request = new ApprovalRequest(
                context.getExecutionId(),
                reels != null ? reels.rawJson() : null,
                context.getSelectedSkill() != null ? context.getSelectedSkill().id() : null
        );
        ApprovalResult result = approvalService.requestApproval(request);

        if (result.status() == ApprovalStatus.PENDING) {
            return NodeResult.success("PENDING")
                    .withMeta("awaitingApproval", true);
        }

        if (result.status() == ApprovalStatus.APPROVED) {
            return NodeResult.success("APPROVED")
                    .withMeta("approvalStatus", "APPROVED")
                    .withMeta("comment", result.comment());
        }

        return NodeResult.failure(result.status().name())
                .withMeta("approvalStatus", result.status())
                .withMeta("comment", result.comment());
    }
}
