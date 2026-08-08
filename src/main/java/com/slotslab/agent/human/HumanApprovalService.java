package com.slotslab.agent.human;

public interface HumanApprovalService {

    ApprovalResult requestApproval(ApprovalRequest request);

    void submitApproval(ApprovalResult result);
}
