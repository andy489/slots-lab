package com.slotslab.agent.human;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stub implementation: auto-approves when no pending approval is found.
 * A real implementation would persist the request and block until a human submits via REST.
 */
@Service
public class DefaultHumanApprovalService implements HumanApprovalService {

    private final Map<UUID, ApprovalResult> pendingApprovals = new ConcurrentHashMap<>();

    @Override
    public ApprovalResult requestApproval(ApprovalRequest request) {
        ApprovalResult pending = pendingApprovals.remove(request.executionId());
        if (pending != null) {
            return pending;
        }
        return new ApprovalResult(ApprovalStatus.PENDING, null);
    }

    @Override
    public void submitApproval(ApprovalResult result) {
        // Wired from REST endpoint — see AgentController
    }

    public void submitApprovalForExecution(UUID executionId, ApprovalResult result) {
        pendingApprovals.put(executionId, result);
    }
}
