package com.slotslab.agent.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotslab.agent.execution.ExecutionTrace;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.skills.ltr.*;
import com.slotslab.agent.tools.LtrReelGenerationTool.MutableState;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Per-run stateful tool-holder for the LTR tuning ReAct loop.
 *
 * One instance is created per generation run and wired into AiServices.
 * The LLM calls these methods directly via LangChain4j tool-calling.
 *
 * Tool call sequence logged to ExecutionTrace for UI display.
 */
public class LtrTuningTools {

    private static final Logger log = LoggerFactory.getLogger(LtrTuningTools.class);

    private final MutableState state;
    private final AgentRequest request;
    private final ReelSetWeightTuner weightTuner;
    private final SymbolCountInitialiser countInit;
    private final SpiralNoWinReelSetFactory noWinFactory;
    private final WinReelSetFactory winFactory;
    private final PaytableGenerator paytableGen;
    private final ObjectMapper mapper;
    private final AgentContext context;

    private long spins;
    private long seed;
    private int toolCallCount = 0;

    // Last simulation result — returned by runSimulation and used for iteration logging
    private ReelSetWeightTuner.SimStats lastStats;
    // All tool calls made in this tuning session
    private final List<ToolCallRecord> toolCallLog = new ArrayList<>();

    public record ToolCallRecord(String tool, String args, String result) {}

    public LtrTuningTools(MutableState state,
                          AgentRequest request,
                          ReelSetWeightTuner weightTuner,
                          SymbolCountInitialiser countInit,
                          SpiralNoWinReelSetFactory noWinFactory,
                          WinReelSetFactory winFactory,
                          PaytableGenerator paytableGen,
                          ObjectMapper mapper,
                          AgentContext context,
                          long spins,
                          long seed) {
        this.state        = state;
        this.request      = request;
        this.weightTuner  = weightTuner;
        this.countInit    = countInit;
        this.noWinFactory = noWinFactory;
        this.winFactory   = winFactory;
        this.paytableGen  = paytableGen;
        this.mapper       = mapper;
        this.context      = context;
        this.spins        = spins;
        this.seed         = seed;
    }

    // ── Tools ─────────────────────────────────────────────────────────────────

    @Tool("Run a simulation with the current state and return rtp, hitRate, maxWin, volatilityLabel.")
    public String runSimulation() {
        lastStats = weightTuner.simulate(
                state.allReelSets(), state.weights, state.paytable,
                state.symbols, state.lines,
                state.minMatch, state.screenWidth, state.screenHeight,
                spins, seed + toolCallCount);
        String result = String.format(
                "{\"rtp\":%.2f,\"hitRate\":%.2f,\"maxWin\":%.2f,\"volatilityLabel\":\"%s\"}",
                lastStats.rtp(), lastStats.hitRate(), lastStats.maxWin(), lastStats.volatilityLabel());
        log("+runSimulation", "", result);
        return result;
    }

    @Tool("""
        Adjust the reel set weight distribution.
        Provide weights as a JSON array of doubles matching the current length (noWinSets + winSets).
        Values are normalised to sum=1.0 automatically. All values must be > 0.
        Example: [0.15, 0.15, 0.15, 0.20, 0.10, 0.25]
        """)
    public String tuneWeights(String weightsJson) {
        try {
            List<Double> list = mapper.readValue(weightsJson, new TypeReference<>() {});
            if (list.size() != state.weights.length)
                return "ERROR: expected " + state.weights.length + " weights, got " + list.size();
            double[] w = new double[list.size()];
            for (int i = 0; i < list.size(); i++) w[i] = list.get(i);
            weightTuner.normalise(w);
            state.weights = w;
            String result = "OK: weights updated (" + w.length + " values)";
            log("tuneWeights", weightsJson, result);
            return result;
        } catch (Exception e) {
            String err = "ERROR: " + e.getMessage();
            log("tuneWeights", weightsJson, err);
            return err;
        }
    }

    @Tool("""
        Patch paytable multipliers for one or more symbols.
        Input: JSON object { "symbolId": { "matchCount": multiplier, ... }, ... }
        Only include symbols you want to change. Sub-1× junior multipliers are valid.
        Example: {"2": {"3": 0.5, "4": 1.5, "5": 5.0}, "3": {"3": 4.0, "4": 14.0, "5": 50.0}}
        """)
    public String patchPaytable(String paytableJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> ptPatch = mapper.readValue(paytableJson, Map.class);
            int patched = 0;
            for (Map.Entry<String, Object> e : ptPatch.entrySet()) {
                int symId;
                try { symId = Integer.parseInt(e.getKey()); }
                catch (NumberFormatException ignored) { continue; }
                if (!(e.getValue() instanceof Map<?, ?> symPatch)) continue;
                Map<Integer, Double> existing = state.paytable.computeIfAbsent(symId, k -> new LinkedHashMap<>());
                for (Map.Entry<?, ?> pe : symPatch.entrySet()) {
                    try {
                        int matchCount = Integer.parseInt(String.valueOf(pe.getKey()));
                        double mult    = ((Number) pe.getValue()).doubleValue();
                        if (request.maxPayout() > 0) mult = Math.min(mult, request.maxPayout());
                        existing.put(matchCount, mult);
                        patched++;
                    } catch (Exception ignored) {}
                }
            }
            String result = "OK: patched " + patched + " multiplier entries across " + ptPatch.size() + " symbols";
            log("patchPaytable", paytableJson, result);
            return result;
        } catch (Exception e) {
            String err = "ERROR: " + e.getMessage();
            log("patchPaytable", paytableJson, err);
            return err;
        }
    }

    @Tool("""
        Trigger a full reel strip rebuild with new structural parameters.
        Use sparingly — this resets paytable and weights to formula defaults.
        symsPerReel: int 64–512 (total tiles per reel)
        winVecDecay: double 0.40–0.95 (win strip tile decay per reel, or -1 to use volatility default)
        targetVolatility: string LOW|CASUAL|HIGH|VERY_HIGH|EXTREME|ULTRA_EXTREME (or empty to keep current)
        """)
    public String rebuildStrips(int symsPerReel, double winVecDecay, String targetVolatility) {
        if (symsPerReel < 64 || symsPerReel > 512)
            return "ERROR: symsPerReel must be 64–512, got " + symsPerReel;
        if (winVecDecay != -1 && (winVecDecay < 0.40 || winVecDecay > 0.95))
            return "ERROR: winVecDecay must be 0.40–0.95 or -1 for default, got " + winVecDecay;

        state.symsPerReel = symsPerReel;
        state.winVecDecay = (winVecDecay == -1) ? Double.NaN : winVecDecay;
        if (targetVolatility != null && !targetVolatility.isBlank())
            state.volatility = targetVolatility;

        state.rebuild(countInit, noWinFactory, winFactory, paytableGen, weightTuner, request);
        String result = String.format(
                "OK: rebuilt strips — symsPerReel=%d winVecDecay=%s volatility=%s noWinSets=%d winSets=%d",
                symsPerReel,
                winVecDecay == -1 ? "volatility-default" : String.valueOf(winVecDecay),
                state.volatility,
                state.noWinSets.size(),
                state.winEntries.size());
        log("rebuildStrips", symsPerReel + "/" + winVecDecay + "/" + targetVolatility, result);
        return result;
    }

    @Tool("Return current targets and tolerances so you can compare against simulation results.")
    public String getTargets() {
        return String.format(
                "{\"targetRtp\":%.2f,\"rtpDelta\":%.2f,\"targetHitRate\":%.2f," +
                "\"hitRateDelta\":%.2f,\"targetVolatility\":\"%s\",\"maxPayout\":%s}",
                request.targetRtp(), request.rtpDelta(),
                request.targetHitRate(), request.hitRateDelta(),
                request.targetVolatility(),
                request.maxPayout() > 0 ? String.valueOf(request.maxPayout()) : "0 (uncapped)");
    }

    @Tool("Return the current weight array and paytable so you can plan precise adjustments.")
    public String getCurrentState() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("weightsLength", state.weights.length);
        List<Double> wList = new ArrayList<>();
        for (double w : state.weights) wList.add(Math.round(w * 10000.0) / 10000.0);
        s.put("weights", wList);
        Map<String, Object> pt = new LinkedHashMap<>();
        state.paytable.forEach((id, pays) -> pt.put(String.valueOf(id), pays));
        s.put("paytable", pt);
        s.put("noWinSetCount", state.noWinSets.size());
        s.put("winSetCount", state.winEntries.size());
        s.put("symsPerReel", state.symsPerReel);
        s.put("volatility", state.volatility);
        try { return mapper.writeValueAsString(s); }
        catch (Exception e) { return s.toString(); }
    }

    // ── accessors ─────────────────────────────────────────────────────────────

    public ReelSetWeightTuner.SimStats getLastStats() { return lastStats; }
    public List<ToolCallRecord> getToolCallLog()      { return Collections.unmodifiableList(toolCallLog); }
    public int getToolCallCount()                     { return toolCallCount; }

    public void setSeed(long seed) { this.seed = seed; }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void log(String tool, String args, String result) {
        toolCallCount++;
        toolCallLog.add(new ToolCallRecord(tool, args, result));
        log.info("[ltr-tools] #{} {} → {}", toolCallCount, tool, result);
        if (context != null) {
            context.getTrace().setStatusMessage(
                    String.format("🔧 Tool call #%d: %s", toolCallCount, tool));
        }
    }
}
