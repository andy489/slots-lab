package com.slotslab.agent.orchestrator;

import com.slotslab.agent.execution.AgentExecution;
import com.slotslab.agent.execution.ExecutionRepository;
import com.slotslab.agent.graph.AgentGraph;
import com.slotslab.agent.graph.GraphExecutor;
import com.slotslab.agent.model.AgentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.function.Function;

/**
 * Runs the agent graph on the async executor. Extracted into its own bean so
 * the {@code @Async} proxy is actually applied — a self-invocation from the
 * orchestrator would bypass the proxy and run synchronously on the HTTP thread.
 */
@Service
public class AgentExecutionRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionRunner.class);

    private final ExecutionRepository executionRepository;

    public AgentExecutionRunner(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @Async("agentExecutor")
    public void runAsync(AgentContext context,
                         AgentExecution execution,
                         Function<AgentContext, AgentGraph> graphFactory,
                         Map<UUID, AgentContext> activeContexts) {
        UUID executionId = context.getExecutionId();
        log.info("executionId={} starting agent", executionId);
        try {
            AgentGraph graph = graphFactory.apply(context);
            new GraphExecutor().execute(graph, context);

            if (context.getReels() != null) {
                execution.setResult(context.getReels().rawJson());
                execution.setStatus(AgentExecution.ExecutionStatus.COMPLETED);
            } else {
                execution.setStatus(AgentExecution.ExecutionStatus.FAILED);
                execution.setError("No reels produced");
            }
        } catch (CancellationException e) {
            log.info("executionId={} cancelled", executionId);
            execution.setStatus(AgentExecution.ExecutionStatus.CANCELLED);
            execution.setError("Cancelled by user");
        } catch (UnsupportedOperationException e) {
            log.warn("executionId={} unsupported strategy: {}", executionId, e.getMessage());
            execution.setStatus(AgentExecution.ExecutionStatus.FAILED);
            execution.setError(e.getMessage());
        } catch (Exception e) {
            log.error("executionId={} agent failed: {}", executionId, e.getMessage());
            execution.setStatus(AgentExecution.ExecutionStatus.FAILED);
            execution.setError(e.getMessage());
        } finally {
            execution.setFinishedAt(Instant.now());
            executionRepository.save(execution);
            activeContexts.remove(executionId);
            log.info("executionId={} finished with status={}", executionId, execution.getStatus());
        }
    }
}
