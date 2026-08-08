package com.slotslab.agent.human;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class HumanFeedback {

    private final ApprovalStatus status;
    private final String comment;
}
