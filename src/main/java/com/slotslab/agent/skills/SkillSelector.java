package com.slotslab.agent.skills;

import com.slotslab.agent.model.AgentRequest;

import java.util.List;

public interface SkillSelector {

    Skill select(AgentRequest request, List<Skill> availableSkills);
}
