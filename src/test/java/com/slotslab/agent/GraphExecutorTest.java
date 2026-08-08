package com.slotslab.agent;

import com.slotslab.agent.execution.ExecutionTrace;
import com.slotslab.agent.graph.*;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GraphExecutorTest {

    private AgentContext makeContext() {
        AgentRequest req = new AgentRequest(95.5, 0.15, 30.0, 5.0, "medium", null, null);
        return new AgentContext(UUID.randomUUID(), req, new ExecutionTrace());
    }

    @Test
    void executesNodesInOrder() {
        List<String> order = new ArrayList<>();

        AgentNode a = new AgentNode() {
            public String getId() { return "A"; }
            public NodeResult execute(AgentContext ctx) { order.add("A"); return NodeResult.success("ok"); }
        };
        AgentNode b = new AgentNode() {
            public String getId() { return "B"; }
            public NodeResult execute(AgentContext ctx) { order.add("B"); return NodeResult.success("ok"); }
        };
        AgentNode c = new AgentNode() {
            public String getId() { return "C"; }
            public NodeResult execute(AgentContext ctx) { order.add("C"); return NodeResult.success("ok"); }
        };

        AgentGraph graph = new AgentGraph()
                .setStartNode("A")
                .addNode(a).addNode(b).addNode(c)
                .addEdge(new AgentEdge("A", "B", null))
                .addEdge(new AgentEdge("B", "C", null));

        new GraphExecutor().execute(graph, makeContext());

        assertEquals(List.of("A", "B", "C"), order);
    }

    @Test
    void conditionalTransitionTakesCorrectBranch() {
        List<String> visited = new ArrayList<>();

        AgentNode start = new AgentNode() {
            public String getId() { return "start"; }
            public NodeResult execute(AgentContext ctx) { return NodeResult.success("go-right"); }
        };
        AgentNode left = new AgentNode() {
            public String getId() { return "left"; }
            public NodeResult execute(AgentContext ctx) { visited.add("left"); return NodeResult.success("ok"); }
        };
        AgentNode right = new AgentNode() {
            public String getId() { return "right"; }
            public NodeResult execute(AgentContext ctx) { visited.add("right"); return NodeResult.success("ok"); }
        };

        AgentGraph graph = new AgentGraph()
                .setStartNode("start")
                .addNode(start).addNode(left).addNode(right)
                .addEdge(new AgentEdge("start", "left",  (ctx, r) -> "go-left".equals(r.getOutcome())))
                .addEdge(new AgentEdge("start", "right", (ctx, r) -> "go-right".equals(r.getOutcome())));

        new GraphExecutor().execute(graph, makeContext());

        assertEquals(List.of("right"), visited);
    }

    @Test
    void stopsWhenNoTransitionMatches() {
        List<String> visited = new ArrayList<>();
        AgentNode a = new AgentNode() {
            public String getId() { return "A"; }
            public NodeResult execute(AgentContext ctx) { visited.add("A"); return NodeResult.success("ok"); }
        };
        AgentNode b = new AgentNode() {
            public String getId() { return "B"; }
            public NodeResult execute(AgentContext ctx) { visited.add("B"); return NodeResult.success("ok"); }
        };

        AgentGraph graph = new AgentGraph()
                .setStartNode("A")
                .addNode(a).addNode(b)
                .addEdge(new AgentEdge("A", "B", (ctx, r) -> false));

        new GraphExecutor().execute(graph, makeContext());

        assertEquals(List.of("A"), visited);
    }

    @Test
    void nodeExceptionIsRecordedAndStops() {
        AgentNode bad = new AgentNode() {
            public String getId() { return "bad"; }
            public NodeResult execute(AgentContext ctx) { throw new RuntimeException("boom"); }
        };
        AgentContext context = makeContext();
        AgentGraph graph = new AgentGraph().setStartNode("bad").addNode(bad);

        assertDoesNotThrow(() -> new GraphExecutor().execute(graph, context));
        assertEquals(1, context.getTrace().getNodes().size());
        assertEquals("boom", context.getTrace().getNodes().get(0).getError());
    }
}
