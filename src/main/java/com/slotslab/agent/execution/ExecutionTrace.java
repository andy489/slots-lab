package com.slotslab.agent.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecutionTrace {

    private final List<NodeExecution> nodes = new ArrayList<>();

    public void record(NodeExecution nodeExecution) {
        nodes.add(nodeExecution);
    }

    public List<NodeExecution> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
}
