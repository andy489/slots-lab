package com.slotslab.agent.orchestrator.nodes;

import com.slotslab.agent.graph.AgentNode;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.model.AgentContext;

public class LoadSkillNode implements AgentNode {

    public static final String ID = "load-skill";

    @Override
    public String getId() { return ID; }

    @Override
    public NodeResult execute(AgentContext context) {
        var skill = context.getSelectedSkill();
        if (skill == null) {
            return NodeResult.failure("No skill selected");
        }
        if (skill.instructions() == null || skill.instructions().isBlank()) {
            return NodeResult.failure("Skill '" + skill.id() + "' has no instructions");
        }
        return NodeResult.success("skill-loaded")
                .withMeta("skill", skill.id())
                .withMeta("instructionsLength", skill.instructions().length());
    }
}
