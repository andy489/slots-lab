package com.slotslab.agent.graph;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.Accessors;

@Getter
@Accessors(chain = true)
public class NodeResult {

  private final NodeStatus status;
  private final String outcome;
  private final String error;
  private final Map<String, Object> metadata = new HashMap<>();

  private NodeResult(NodeStatus status, String outcome, String error) {
    this.status = status;
    this.outcome = outcome;
    this.error = error;
  }

  public static NodeResult success(String outcome) {
    return new NodeResult(NodeStatus.SUCCESS, outcome, null);
  }

  public static NodeResult failure(String error) {
    return new NodeResult(NodeStatus.FAILURE, null, error);
  }

  public NodeResult withMeta(String key, Object value) {
    metadata.put(key, value);
    return this;
  }

  public boolean isSuccess() {
    return status == NodeStatus.SUCCESS;
  }
}
