package com.slotslab.agent.orchestrator.nodes;

import com.slotslab.agent.graph.AgentNode;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.validation.ReelValidator;
import com.slotslab.agent.validation.ValidationResult;

public class ValidateReelsNode implements AgentNode {

    public static final String ID = "validate-reels";

    private final ReelValidator validator;

    public ValidateReelsNode(ReelValidator validator) {
        this.validator = validator;
    }

    @Override
    public String getId() { return ID; }

    @Override
    public NodeResult execute(AgentContext context) {
        ValidationResult result = validator.validate(context.getReels(), context);
        if (result.isValid()) {
            return NodeResult.success("VALID");
        }
        return NodeResult.failure(result.getReason())
                .withMeta("validationError", result.getReason());
    }
}
