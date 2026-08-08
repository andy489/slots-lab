package com.slotslab.agent.tools;

import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.model.GeneratedReels;

public interface AgentReelGenerationTool {

    /** Returns true if this tool supports the given payout strategy. */
    boolean supports(String strategy);

    GeneratedReels generate(AgentRequest request);

    default GeneratedReels generate(AgentRequest request, AgentContext context) {
        return generate(request);
    }
}
