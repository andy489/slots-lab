package com.slotslab.agent.graph;

import com.slotslab.agent.model.AgentContext;

public interface AgentNode {

    String getId();

    NodeResult execute(AgentContext context);
}
