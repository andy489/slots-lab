package com.slotslab.agent.graph;

public record AgentEdge(String fromNodeId, String toNodeId, TransitionCondition condition) {
}
