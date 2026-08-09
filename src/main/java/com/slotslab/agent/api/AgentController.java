package com.slotslab.agent.api;

import com.slotslab.agent.api.dto.AgentExecutionResponse;
import com.slotslab.agent.api.dto.AgentRunRequest;
import com.slotslab.agent.execution.AgentExecution;
import com.slotslab.agent.execution.ExecutionRepository;
import com.slotslab.agent.llm.LlmService;
import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.orchestrator.AgentOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

@Tag(name = "AI Agent", description = "Skill-based agent for automated reel generation")
@RestController
@RequestMapping("/api/agent")
public class AgentController {

  private final AgentOrchestrator orchestrator;
  private final ExecutionRepository executionRepository;
  private final AgentRequestValidator validator;

  public AgentController(AgentOrchestrator orchestrator,
      ExecutionRepository executionRepository,
      AgentRequestValidator validator) {
    this.orchestrator = orchestrator;
    this.executionRepository = executionRepository;
    this.validator = validator;
  }

  @Operation(summary = "Start agent execution (async)", description = "Submits a generation job and returns immediately with executionId and RUNNING status.")
  @PostMapping("/generate")
  public ResponseEntity<?> generate(@Valid @RequestBody AgentRunRequest request) {
    List<String> errors = validator.validate(request);
    if (!errors.isEmpty()) {
      return ResponseEntity.badRequest().body(errors);
    }
    AgentRequest agentRequest = new AgentRequest(
        request.targetRtp(),
        request.rtpDelta(),
        request.targetHitRate(),
        request.hitRateDelta(),
        request.targetVolatility(),
        request.maxPayout(),
        request.reelConfig(),
        request.parameters()
    );
    try {
      AgentExecution execution = orchestrator.submit(agentRequest);
      return ResponseEntity.accepted().body(AgentExecutionResponse.from(execution));
    } catch (RejectedExecutionException e) {
      return ResponseEntity.status(503)
          .body("Agent execution queue is full — try again shortly");
    }
  }

  @Operation(summary = "Poll execution status", description = "Returns the current state of an execution.")
  @GetMapping("/executions/{executionId}")
  public ResponseEntity<AgentExecutionResponse> getExecution(@PathVariable UUID executionId) {
    return executionRepository.findById(executionId)
        .map(ex -> ResponseEntity.ok(AgentExecutionResponse.from(ex)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Cancel execution", description = "Sends a cancel signal to a running execution.")
  @PostMapping("/executions/{executionId}/cancel")
  public ResponseEntity<Void> cancel(@PathVariable UUID executionId) {
    orchestrator.cancel(executionId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Get LLM debug file", description = "Returns the prompt+response JSON for a specific iteration.")
  @GetMapping("/executions/{executionId}/debug/{iteration}")
  public ResponseEntity<String> getDebug(@PathVariable UUID executionId,
                                         @PathVariable int iteration) {
    Path file = LlmService.getDebugFilePath(executionId, iteration);
    if (!Files.exists(file)) return ResponseEntity.notFound().build();
    try {
      return ResponseEntity.ok()
          .header("Content-Type", "application/json")
          .body(Files.readString(file));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
