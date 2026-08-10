package com.slotslab.agent.llm;

import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.prompts.PlanPromptBuilder;
import com.slotslab.agent.skills.FileSystemSkillRegistry;
import com.slotslab.agent.skills.Skill;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    @Value("${env.OPENAI_API_KEY:${OPENAI_API_KEY:}}")
    private String apiKey;

    private final FileSystemSkillRegistry skillRegistry;
    private final ObjectMapper objectMapper;
    private ChatModel model;

    public LlmService(FileSystemSkillRegistry skillRegistry, ObjectMapper objectMapper) {
        this.skillRegistry = skillRegistry;
        this.objectMapper  = objectMapper;
    }

    @PostConstruct
    void init() {
        if (apiKey == null || apiKey.isBlank()) apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) apiKey = readDotEnv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY is not set — LLM features will be disabled");
            return;
        }
        model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .temperature(0.0)
                .build();
        log.info("LlmService initialised with model={}", OpenAiChatModelName.GPT_4_O_MINI);
    }

    public boolean isAvailable() {
        return model != null;
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Initial planning call. Returns a JSON object with starting parameters
     * (symsPerReel, volatility, symbols, lines, maxIterations, paytable, weights, etc.).
     */
    public String plan(AgentRequest request, AgentContext context) {
        String skillDocs = loadSkillDocs("ltr");
        String paramsJson = toJson(request.parameters());
        int winSymCount   = countWinSymbols(request.parameters());
        int suggestedIter = (int) Math.clamp((long) winSymCount * 20, 80, 200);

        PlanPromptBuilder builder = new PlanPromptBuilder(skillDocs);
        String prompt = builder.build(request, paramsJson, winSymCount, suggestedIter);
        // Plan uses json_object response format for reliable JSON output
        ChatModel planModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .temperature(0.0)
                .responseFormat("json_object")
                .build();
        String response = planModel.chat(prompt);

        if (context != null) {
            context.addLlmCall(0, prompt, response);
            writeDebugFile(context.getExecutionId(), 0, prompt, response);
        }
        return response;
    }

    /**
     * Creates a TuningAgent backed by AiServices + the given LtrTuningTools.
     * The agent calls tools autonomously (ReAct) until convergence or budget exhausted.
     */
    public TuningAgent createTuningAgent(LtrTuningTools tools) {
        return AiServices.builder(TuningAgent.class)
                .chatModel(model)
                .tools(tools)
                .build();
    }

    private void writeDebugFile(UUID executionId, int iteration, String prompt, String response) {
        try {
            Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "agent-debug");
            Files.createDirectories(dir);
            Path file = dir.resolve(executionId + "-iter" + iteration + ".json");
            Map<String, Object> payload = Map.of(
                "executionId", executionId.toString(),
                "iteration", iteration,
                "prompt", prompt,
                "response", response
            );
            Files.writeString(file, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            log.debug("Debug file written: {}", file);
        } catch (IOException e) {
            log.debug("Could not write debug file: {}", e.getMessage());
        }
    }

    public static Path getDebugFilePath(UUID executionId, int iteration) {
        return Paths.get(System.getProperty("java.io.tmpdir"), "agent-debug",
                executionId + "-iter" + iteration + ".json");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String loadSkillDocs(String strategy) {
        List<Skill> skills = skillRegistry.getSkillsForStrategy(strategy);
        log.debug("Loaded {} skill docs for strategy={}", skills.size(), strategy);
        return skills.stream()
                .map(s -> "### " + s.name() + " (" + s.id() + ")\n" + s.instructions())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    public String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (Exception e) {
            return map.toString();
        }
    }

    @SuppressWarnings("unchecked")
    public int countWinSymbols(Map<String, Object> params) {
        if (params == null) return 4;
        Object raw = params.get("symbols");
        if (!(raw instanceof List<?> list)) return 4;
        int count = 0;
        for (Object item : list) {
            if (item instanceof Map<?,?> sym) {
                Object tierObj = sym.get("tier");
                String tier = tierObj != null ? String.valueOf(tierObj) : "";
                if (tier.equals("junior") || tier.equals("senior")) count++;
            }
        }
        return count > 0 ? count : 4;
    }

    public double extractRtp(String simulationJson) {
        if (simulationJson == null || simulationJson.isBlank()) return 0;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = objectMapper.readValue(simulationJson, Map.class);
            Object sim = root.get("simulation");
            if (sim instanceof Map<?,?> simMap) {
                Object rtp = simMap.get("rtp");
                if (rtp instanceof Number n) return n.doubleValue();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private String readDotEnv(String key) {
        try {
            java.nio.file.Path envFile = java.nio.file.Paths.get(".env");
            if (!java.nio.file.Files.exists(envFile)) return null;
            for (String line : java.nio.file.Files.readAllLines(envFile)) {
                line = line.strip();
                if (line.startsWith(key + "=")) {
                    String value = line.substring(key.length() + 1).strip();
                    return value.isBlank() ? null : value;
                }
            }
        } catch (Exception e) {
            log.debug("Could not read .env file: {}", e.getMessage());
        }
        return null;
    }
}
