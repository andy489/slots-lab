package com.slotslab.agent.validation;

import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.GeneratedReels;

public interface ReelValidator {

    ValidationResult validate(GeneratedReels reels, AgentContext context);
}
