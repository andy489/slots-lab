package com.slotslab.agent.model;

import com.slotslab.agent.execution.ExecutionTrace;
import com.slotslab.agent.human.HumanFeedback;
import com.slotslab.agent.skills.Skill;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Accessors(chain = true)
public class AgentContext {

  private final UUID executionId;
  private final AgentRequest request;
  private final ExecutionTrace trace;
  private final Map<String, Object> variables = new HashMap<>();
  private final Map<String, Object> retryHints = new HashMap<>();
  private final List<LlmCallRecord> llmHistory = new ArrayList<>();

  public record LlmCallRecord(int iteration, String prompt, String response) {}

  public synchronized void addLlmCall(int iteration, String prompt, String response) {
    llmHistory.add(new LlmCallRecord(iteration, prompt, response));
  }

  public synchronized List<LlmCallRecord> getLlmHistory() {
    return Collections.unmodifiableList(llmHistory);
  }

  @Setter
  private Skill selectedSkill;
  @Setter
  private GeneratedReels reels;
  @Setter
  private HumanFeedback humanFeedback;

  private int attempts;
  private volatile boolean cancelled;

  public AgentContext(UUID executionId, AgentRequest request, ExecutionTrace trace) {
    this.executionId = executionId;
    this.request = request;
    this.trace = trace;
  }

  public void incrementAttempts() {
    this.attempts++;
  }

  public Map<String, Object> getRetryHints() {
    return retryHints;
  }

  public void putRetryHint(String key, Object value) {
    retryHints.put(key, value);
  }

  public void cancel() {
    this.cancelled = true;
  }

  public boolean isCancelled() {
    return cancelled;
  }
}
