package com.slotslab.agent.orchestrator;

import com.slotslab.agent.execution.AgentExecution;
import com.slotslab.agent.model.AgentRequest;

import java.util.UUID;

public interface AgentOrchestrator {

    /** Submits request for async execution; returns the execution stub immediately. */
    AgentExecution submit(AgentRequest request);

    /** Signals the running execution to cancel. */
    void cancel(UUID executionId);
}
