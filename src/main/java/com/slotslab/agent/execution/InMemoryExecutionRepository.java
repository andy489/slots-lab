package com.slotslab.agent.execution;

import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InMemoryExecutionRepository implements ExecutionRepository {

    private static final int MAX_ENTRIES = 200;

    private final Map<UUID, AgentExecution> store = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, AgentExecution> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });

    @Override
    public AgentExecution save(AgentExecution execution) {
        store.put(execution.getExecutionId(), execution);
        return execution;
    }

    @Override
    public Optional<AgentExecution> findById(UUID executionId) {
        synchronized (store) {
            return Optional.ofNullable(store.get(executionId));
        }
    }
}
