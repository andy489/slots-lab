package com.slotslab.agent.graph;

import com.slotslab.agent.model.AgentContext;

@FunctionalInterface
public interface TransitionCondition {

  boolean evaluate(AgentContext context, NodeResult result);
}
