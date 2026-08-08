package com.slotslab.agent.execution;

import java.util.Optional;
import java.util.UUID;

public interface ExecutionRepository {

    AgentExecution save(AgentExecution execution);

    Optional<AgentExecution> findById(UUID executionId);
}
