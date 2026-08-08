package com.slotslab.agent.execution;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Accessors(chain = true)
public class AgentExecution {

  public enum ExecutionStatus {RUNNING, COMPLETED, FAILED, CANCELLED, MAX_ATTEMPTS_EXCEEDED}

  private final UUID executionId;
  private final Instant startedAt;
  private final ExecutionTrace trace;
  private Instant finishedAt;
  private ExecutionStatus status = ExecutionStatus.RUNNING;
  private String result;
  private String error;

  public AgentExecution(UUID executionId, Instant startedAt, ExecutionTrace trace) {
    this.executionId = executionId;
    this.startedAt = startedAt;
    this.trace = trace;
  }
}
