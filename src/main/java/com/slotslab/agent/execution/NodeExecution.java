package com.slotslab.agent.execution;

import com.slotslab.agent.graph.NodeResult;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Accessors(chain = true)
public class NodeExecution {

    public enum NodeStatus { RUNNING, SUCCESS, FAILURE }

    private final String nodeId;
    private final Instant startedAt;
    @Setter private Instant finishedAt;
    @Setter private NodeStatus status = NodeStatus.RUNNING;
    @Setter private Object input;
    @Setter private Object output;
    @Setter private String error;
    private final Map<String, Object> metadata = new HashMap<>();

    public NodeExecution(String nodeId, Instant startedAt) {
        this.nodeId = nodeId;
        this.startedAt = startedAt;
    }

    public void complete(NodeResult result) {
        this.finishedAt = Instant.now();
        this.status = result.isSuccess() ? NodeStatus.SUCCESS : NodeStatus.FAILURE;
        this.output = result.getOutcome();
        this.error = result.getError();
        this.metadata.putAll(result.getMetadata());
    }

    public void completeWithError(String errorMessage) {
        this.finishedAt = Instant.now();
        this.status = NodeStatus.FAILURE;
        this.error = errorMessage;
    }
}
