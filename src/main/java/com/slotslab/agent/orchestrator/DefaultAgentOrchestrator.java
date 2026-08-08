package com.slotslab.agent.orchestrator;

import com.slotslab.agent.execution.AgentExecution;
import com.slotslab.agent.execution.ExecutionRepository;
import com.slotslab.agent.execution.ExecutionTrace;
import com.slotslab.agent.graph.AgentEdge;
import com.slotslab.agent.graph.AgentGraph;
import com.slotslab.agent.graph.GraphExecutor;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.orchestrator.nodes.*;
import com.slotslab.agent.skills.AgentProperties;
import com.slotslab.agent.skills.SkillRegistry;
import com.slotslab.agent.skills.SkillSelector;
import com.slotslab.agent.tools.AgentReelGenerationTool;
import com.slotslab.agent.validation.ReelValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultAgentOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DefaultAgentOrchestrator.class);

    private final SkillRegistry skillRegistry;
    private final SkillSelector skillSelector;
    private final List<AgentReelGenerationTool> generationTools;
    private final ReelValidator reelValidator;
    private final ExecutionRepository executionRepository;
    private final AgentProperties properties;

    /** Active contexts keyed by executionId — used for cancellation. */
    private final Map<UUID, AgentContext> activeContexts = new ConcurrentHashMap<>();

    public DefaultAgentOrchestrator(
            SkillRegistry skillRegistry,
            SkillSelector skillSelector,
            List<AgentReelGenerationTool> generationTools,
            ReelValidator reelValidator,
            ExecutionRepository executionRepository,
            AgentProperties properties) {
        this.skillRegistry    = skillRegistry;
        this.skillSelector    = skillSelector;
        this.generationTools  = generationTools;
        this.reelValidator    = reelValidator;
        this.executionRepository = executionRepository;
        this.properties       = properties;
    }

    @Override
    public AgentExecution submit(AgentRequest request) {
        UUID executionId = UUID.randomUUID();
        ExecutionTrace trace = new ExecutionTrace();
        AgentContext context = new AgentContext(executionId, request, trace);
        AgentExecution execution = new AgentExecution(executionId, Instant.now(), trace);
        executionRepository.save(execution);
        activeContexts.put(executionId, context);
        runAsync(context, execution);
        return execution;
    }

    @Override
    public void cancel(UUID executionId) {
        AgentContext ctx = activeContexts.get(executionId);
        if (ctx != null) {
            ctx.cancel();
            log.info("executionId={} cancel signal sent", executionId);
        }
    }

    @Async("agentExecutor")
    protected void runAsync(AgentContext context, AgentExecution execution) {
        UUID executionId = context.getExecutionId();
        log.info("executionId={} starting agent", executionId);
        try {
            AgentGraph graph = buildGraph(context);
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

    private AgentGraph buildGraph(AgentContext context) {
        int maxAttempts = resolveMaxAttempts(context.getRequest());

        var analyzeNode   = new AnalyzeRequestNode();
        var selectSkill   = new SkillSelectionNode(skillRegistry, skillSelector);
        var loadSkill     = new LoadSkillNode();
        var generateReels = new GenerateReelsNode(generationTools);
        var validateReels = new ValidateReelsNode(reelValidator);
        var finalize      = new FinalizeNode();

        return new AgentGraph()
                .setStartNode(AnalyzeRequestNode.ID)
                .addNode(analyzeNode)
                .addNode(selectSkill)
                .addNode(loadSkill)
                .addNode(generateReels)
                .addNode(validateReels)
                .addNode(finalize)
                .addEdge(new AgentEdge(AnalyzeRequestNode.ID,  SkillSelectionNode.ID, (ctx, r) -> r.isSuccess()))
                .addEdge(new AgentEdge(SkillSelectionNode.ID,  LoadSkillNode.ID,      (ctx, r) -> r.isSuccess()))
                .addEdge(new AgentEdge(LoadSkillNode.ID,       GenerateReelsNode.ID,  null))
                .addEdge(new AgentEdge(GenerateReelsNode.ID,   ValidateReelsNode.ID,  (ctx, r) -> r.isSuccess()))
                .addEdge(new AgentEdge(ValidateReelsNode.ID,   FinalizeNode.ID,
                        (ctx, r) -> r.isSuccess() && "VALID".equals(r.getOutcome())))
                .addEdge(new AgentEdge(ValidateReelsNode.ID,   GenerateReelsNode.ID,
                        (ctx, r) -> !r.isSuccess() && ctx.getAttempts() < maxAttempts));
    }

    private int resolveMaxAttempts(AgentRequest request) {
        if (request.parameters() != null) {
            Object v = request.parameters().get("maxAttempts");
            if (v instanceof Number n) {
                int val = n.intValue();
                if (val >= 1 && val <= 20) return val;
            }
        }
        return properties.getMaxAttempts();
    }
}
