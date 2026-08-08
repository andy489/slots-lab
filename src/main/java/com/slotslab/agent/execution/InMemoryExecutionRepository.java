package com.slotslab.agent.execution;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryExecutionRepository implements ExecutionRepository {

    private final Map<UUID, AgentExecution> store = new ConcurrentHashMap<>();

    @Override
    public AgentExecution save(AgentExecution execution) {
        store.put(execution.getExecutionId(), execution);
        return execution;
    }

    @Override
    public Optional<AgentExecution> findById(UUID executionId) {
        return Optional.ofNullable(store.get(executionId));
    }
}
