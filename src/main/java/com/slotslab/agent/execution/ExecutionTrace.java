package com.slotslab.agent.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExecutionTrace {

    public record IterationLog(
            int iteration,
            int maxIterations,
            double rtp,
            double hitRate,
            double stdDev,
            boolean converged,
            Map<String, Object> llmPatch,      // null on first sim before any LLM call
            Map<String, Object> hitDistribution // win-combo distribution for this iteration; may be null
    ) {}

    private final List<NodeExecution> nodes = new ArrayList<>();
    private final List<IterationLog> iterations = new ArrayList<>();
    private volatile String statusMessage = null;
    private volatile Map<String, Object> initialPlan = null;
    private volatile Map<String, Object> initialState = null;

    public void setInitialState(Map<String, Object> state) {
        this.initialState = state;
    }

    public Map<String, Object> getInitialState() {
        return initialState;
    }

    public void setInitialPlan(Map<String, Object> plan) {
        this.initialPlan = plan;
    }

    public Map<String, Object> getInitialPlan() {
        return initialPlan;
    }

    public void record(NodeExecution nodeExecution) {
        nodes.add(nodeExecution);
    }

    public synchronized void addIterationLog(IterationLog log) {
        iterations.add(log);
    }

    public void setStatusMessage(String message) {
        this.statusMessage = message;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public List<NodeExecution> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public synchronized List<IterationLog> getIterations() {
        return List.copyOf(iterations);
    }
}
