package com.slotslab.agent.skills;

import com.slotslab.agent.model.AgentRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultSkillSelector implements SkillSelector {

    private static final Set<String> SUPPORTED_STRATEGIES = Set.of("LTR");

    @Override
    public Skill select(AgentRequest request, List<Skill> strategySkills) {
        String strategy = resolveStrategy(request);

        if (!SUPPORTED_STRATEGIES.contains(strategy)) {
            throw new UnsupportedOperationException(
                "Strategy '" + strategy + "' is not supported. " +
                "Currently supported strategies: " + SUPPORTED_STRATEGIES + ".");
        }

        // strategySkills is already filtered + sorted by ID (01-, 02-, ...) by the registry
        // Return the first skill as the representative for the strategy
        return strategySkills.get(0);
    }

    private String resolveStrategy(AgentRequest request) {
        Map<String, Object> params = request.parameters();
        if (params != null && params.get("strategy") instanceof String s) {
            return s.toUpperCase();
        }
        return "LTR";
    }
}
