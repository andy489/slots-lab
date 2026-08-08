package com.slotslab.agent.orchestrator.nodes;

import com.slotslab.agent.graph.AgentNode;
import com.slotslab.agent.graph.NodeResult;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.skills.Skill;
import com.slotslab.agent.skills.SkillRegistry;
import com.slotslab.agent.skills.SkillSelector;

import java.util.List;

public class SkillSelectionNode implements AgentNode {

    public static final String ID = "select-skill";

    private final SkillRegistry registry;
    private final SkillSelector selector;

    public SkillSelectionNode(SkillRegistry registry, SkillSelector selector) {
        this.registry = registry;
        this.selector = selector;
    }

    @Override
    public String getId() { return ID; }

    @Override
    public NodeResult execute(AgentContext context) {
        String strategy = (String) context.getVariables()
                .getOrDefault(AnalyzeRequestNode.KEY_STRATEGY, "LTR");

        List<Skill> strategySkills = registry.getSkillsForStrategy(strategy);
        if (strategySkills.isEmpty()) {
            return NodeResult.failure(
                "No skills found for strategy '" + strategy + "'. " +
                "Add skill definitions under skills/" + strategy.toLowerCase() + "/");
        }

        Skill skill = selector.select(context.getRequest(), strategySkills);
        context.setSelectedSkill(skill);
        return NodeResult.success(skill.id())
                .withMeta("skill", skill.id())
                .withMeta("skillName", skill.name())
                .withMeta("strategy", strategy)
                .withMeta("strategySkillCount", strategySkills.size());
    }
}
