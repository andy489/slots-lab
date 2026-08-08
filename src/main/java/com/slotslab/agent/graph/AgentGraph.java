package com.slotslab.agent.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(chain = true)
public class AgentGraph {

  private final Map<String, AgentNode> nodes = new LinkedHashMap<>();
  private final List<AgentEdge> edges = new ArrayList<>();
  private String startNodeId;

  public AgentGraph addNode(AgentNode node) {
    nodes.put(node.getId(), node);
    return this;
  }

  public AgentGraph addEdge(AgentEdge edge) {
    edges.add(edge);
    return this;
  }

  public AgentGraph setStartNode(String nodeId) {
    this.startNodeId = nodeId;
    return this;
  }

  public AgentNode getNode(String id) {
    return nodes.get(id);
  }

  public List<AgentEdge> getEdgesFrom(String nodeId) {
    return edges.stream().filter(e -> e.fromNodeId().equals(nodeId)).toList();
  }
}
