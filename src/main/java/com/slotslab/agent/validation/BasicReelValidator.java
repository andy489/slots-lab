package com.slotslab.agent.validation;

import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.GeneratedReels;
import org.springframework.stereotype.Component;

@Component
public class BasicReelValidator implements ReelValidator {

    @Override
    public ValidationResult validate(GeneratedReels reels, AgentContext context) {
        if (reels == null) {
            return ValidationResult.fail("No reels generated");
        }
        if (reels.rawJson() == null || reels.rawJson().isBlank()) {
            return ValidationResult.fail("Generated reel JSON is empty");
        }
        if (reels.config() == null || reels.config().reelSets() == null || reels.config().reelSets().isEmpty()) {
            return ValidationResult.fail("Reel config has no reel sets");
        }
        return ValidationResult.ok();
    }
}
