package com.slotslab.agent.api.dto;

import com.slotslab.agent.execution.AgentExecution;
import com.slotslab.agent.execution.ExecutionTrace;
import com.slotslab.agent.execution.NodeExecution;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AgentExecutionResponse(
        UUID executionId,
        String status,
        Instant startedAt,
        Instant finishedAt,
        String result,
        String error,
        List<NodeExecutionView> nodes,
        List<ExecutionTrace.IterationLog> iterations,
        String statusMessage,
        Map<String, Object> initialPlan,
        Map<String, Object> initialState
) {
    public record NodeExecutionView(
            String nodeId,
            String status,
            Instant startedAt,
            Instant finishedAt,
            Object output,
            String error,
            Map<String, Object> metadata
    ) {}

    public static AgentExecutionResponse from(AgentExecution ex) {
        List<NodeExecutionView> nodes = ex.getTrace().getNodes().stream()
                .map(n -> new NodeExecutionView(
                        n.getNodeId(),
                        n.getStatus().name(),
                        n.getStartedAt(),
                        n.getFinishedAt(),
                        n.getOutput(),
                        n.getError(),
                        n.getMetadata()
                )).toList();
        return new AgentExecutionResponse(
                ex.getExecutionId(),
                ex.getStatus().name(),
                ex.getStartedAt(),
                ex.getFinishedAt(),
                ex.getResult(),
                ex.getError(),
                nodes,
                ex.getTrace().getIterations(),
                ex.getTrace().getStatusMessage(),
                ex.getTrace().getInitialPlan(),
                ex.getTrace().getInitialState()
        );
    }
}
