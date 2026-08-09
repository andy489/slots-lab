package com.slotslab.agent.orchestrator;

import com.slotslab.agent.execution.AgentExecution;
import com.slotslab.agent.execution.ExecutionRepository;
import com.slotslab.agent.execution.ExecutionTrace;
import com.slotslab.agent.graph.AgentEdge;
import com.slotslab.agent.graph.AgentGraph;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.orchestrator.nodes.AnalyzeRequestNode;
import com.slotslab.agent.orchestrator.nodes.FinalizeNode;
import com.slotslab.agent.orchestrator.nodes.GenerateReelsNode;
import com.slotslab.agent.tools.AgentReelGenerationTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

@Service
public class DefaultAgentOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DefaultAgentOrchestrator.class);

    private final List<AgentReelGenerationTool> generationTools;
    private final ExecutionRepository executionRepository;
    private final AgentExecutionRunner runner;

    private final Map<UUID, AgentContext> activeContexts = new ConcurrentHashMap<>();

    public DefaultAgentOrchestrator(List<AgentReelGenerationTool> generationTools,
                                     ExecutionRepository executionRepository,
                                     AgentExecutionRunner runner) {
        this.generationTools     = generationTools;
        this.executionRepository = executionRepository;
        this.runner              = runner;
    }

    @Override
    public AgentExecution submit(AgentRequest request) {
        UUID executionId = UUID.randomUUID();
        ExecutionTrace trace = new ExecutionTrace();
        AgentContext context = new AgentContext(executionId, request, trace);
        AgentExecution execution = new AgentExecution(executionId, Instant.now(), trace);
        executionRepository.save(execution);
        activeContexts.put(executionId, context);
        try {
            runner.runAsync(context, execution, this::buildGraph, activeContexts);
        } catch (RejectedExecutionException e) {
            activeContexts.remove(executionId);
            log.warn("executionId={} rejected — agent executor saturated", executionId);
            throw e;
        }
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

    private AgentGraph buildGraph(AgentContext context) {
        var analyzeNode   = new AnalyzeRequestNode();
        var generateReels = new GenerateReelsNode(generationTools);
        var finalize      = new FinalizeNode();

        return new AgentGraph()
                .setStartNode(AnalyzeRequestNode.ID)
                .addNode(analyzeNode)
                .addNode(generateReels)
                .addNode(finalize)
                .addEdge(new AgentEdge(AnalyzeRequestNode.ID, GenerateReelsNode.ID, (ctx, r) -> r.isSuccess()))
                .addEdge(new AgentEdge(GenerateReelsNode.ID,  FinalizeNode.ID,       null));
    }
}
