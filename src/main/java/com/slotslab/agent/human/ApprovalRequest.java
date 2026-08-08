package com.slotslab.agent.human;

import java.util.UUID;

public record ApprovalRequest(
        UUID executionId,
        String reelsJson,
        String skillUsed
) {}
