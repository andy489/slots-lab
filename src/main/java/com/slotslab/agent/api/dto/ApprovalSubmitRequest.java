package com.slotslab.agent.api.dto;

import com.slotslab.agent.human.ApprovalStatus;

public record ApprovalSubmitRequest(
        ApprovalStatus status,
        String comment
) {}
