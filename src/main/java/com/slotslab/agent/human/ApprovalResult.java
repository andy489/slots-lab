package com.slotslab.agent.human;

public record ApprovalResult(
        ApprovalStatus status,
        String comment
) {}
