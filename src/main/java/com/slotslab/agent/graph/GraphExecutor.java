package com.slotslab.agent.graph;

import com.slotslab.agent.execution.NodeExecution;
import com.slotslab.agent.model.AgentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CancellationException;

public class GraphExecutor {

  private static final Logger log = LoggerFactory.getLogger(GraphExecutor.class);

  public void execute(AgentGraph graph, AgentContext context) {
    String currentNodeId = graph.getStartNodeId();

    while (currentNodeId != null) {
      if (context.isCancelled()) {
        log.info("executionId={} cancelled before node='{}'", context.getExecutionId(), currentNodeId);
        throw new CancellationException("Execution cancelled by user");
      }

      AgentNode node = graph.getNode(currentNodeId);
      if (node == null) {
        log.warn("executionId={} node '{}' not found in graph, stopping", context.getExecutionId(),
            currentNodeId);
        break;
      }

      log.debug("executionId={} executing node='{}'", context.getExecutionId(), currentNodeId);
      NodeExecution nodeExec = new NodeExecution(currentNodeId, Instant.now());
      NodeResult result;
      try {
        result = node.execute(context);
        nodeExec.complete(result);
      } catch (CancellationException ex) {
        throw ex;
      } catch (Exception ex) {
        log.error("executionId={} node='{}' threw exception: {}", context.getExecutionId(),
            currentNodeId, ex.getMessage());
        result = NodeResult.failure(ex.getMessage());
        nodeExec.completeWithError(ex.getMessage());
      }
      context.getTrace().record(nodeExec);

      currentNodeId = resolveNextNode(graph, context, currentNodeId, result);
    }
  }

  private String resolveNextNode(AgentGraph graph, AgentContext context, String fromId,
      NodeResult result) {
    List<AgentEdge> edges = graph.getEdgesFrom(fromId);
    for (AgentEdge edge : edges) {
      if (edge.condition() == null || edge.condition().evaluate(context, result)) {
        return edge.toNodeId();
      }
    }
    return null;
  }
}
