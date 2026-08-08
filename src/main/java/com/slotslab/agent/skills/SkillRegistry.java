package com.slotslab.agent.skills;

import java.util.List;

public interface SkillRegistry {

    Skill getSkill(String skillId);

    List<Skill> getAvailableSkills();

    /** Returns all skills whose ID starts with {@code strategy/} (case-insensitive). */
    List<Skill> getSkillsForStrategy(String strategy);
}
