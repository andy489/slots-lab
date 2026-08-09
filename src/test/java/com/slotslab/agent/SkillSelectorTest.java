package com.slotslab.agent;

import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.skills.DefaultSkillSelector;
import com.slotslab.agent.skills.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillSelectorTest {

    private Skill skill(String id) {
        return new Skill(id, id, "", null, "instructions");
    }

    private AgentRequest requestFor(String volatility) {
        return new AgentRequest(95.5, 0.15, 30.0, 5.0, volatility, 0.0, null, null);
    }

    @Test
    void selectsSkillMatchingVolatility() {
        DefaultSkillSelector selector = new DefaultSkillSelector();
        List<Skill> skills = List.of(skill("balanced-payout"), skill("high-volatility"), skill("low-volatility"));

        Skill selected = selector.select(requestFor("high"), skills);

        assertEquals("high-volatility", selected.id());
    }

    @Test
    void fallsBackToFirstSkillWhenNoMatch() {
        DefaultSkillSelector selector = new DefaultSkillSelector();
        List<Skill> skills = List.of(skill("balanced-payout"), skill("high-volatility"));

        Skill selected = selector.select(requestFor("extreme"), skills);

        assertEquals("balanced-payout", selected.id());
    }

    @Test
    void throwsWhenNoSkillsAvailable() {
        DefaultSkillSelector selector = new DefaultSkillSelector();
        assertThrows(IllegalStateException.class,
                () -> selector.select(requestFor("medium"), List.of()));
    }
}
