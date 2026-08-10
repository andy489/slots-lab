package com.slotslab.agent.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotslab.agent.execution.ExecutionTrace;
import com.slotslab.agent.llm.LlmService;
import com.slotslab.agent.llm.LtrTuningTools;
import com.slotslab.agent.llm.TuningAgent;
import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;
import com.slotslab.agent.model.GeneratedReels;
import com.slotslab.agent.skills.ltr.*;
import com.slotslab.reel.ReelSet;
import com.slotslab.reel.ReelSetsCollectionData;
import com.slotslab.reel.Restriction;
import com.slotslab.reel.Strategy;
import com.slotslab.shuffler.ShuffleGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * LTR reel generation tool — ReAct (tool-use) architecture.
 *
 * Flow:
 *   1. LLM Plan (1 call, json_object mode) → initial structural params + paytable + weights
 *   2. Build strips from plan
 *   3. Simulate iteration 0 — check early convergence
 *   4. For each remaining iteration:
 *        a. Create LtrTuningTools (stateful tool-holder for this iteration)
 *        b. Create TuningAgent backed by AiServices + tools
 *        c. Agent calls runSimulation / tuneWeights / patchPaytable / rebuildStrips autonomously
 *        d. After agent returns, read last sim stats from tools; log to trace
 *   5. Return best result across all iterations
 */
@Component
public class LtrReelGenerationTool implements AgentReelGenerationTool {

    private static final Logger log = LoggerFactory.getLogger(LtrReelGenerationTool.class);

    private final SymbolCountInitialiser countInit;
    private final SpiralNoWinReelSetFactory noWinFactory;
    private final WinReelSetFactory winFactory;
    private final PaytableGenerator paytableGen;
    private final ReelSetWeightTuner weightTuner;
    private final RestrictionBuilder restrictionBuilder;
    private final LlmService llmService;
    private final ObjectMapper mapper;

    public LtrReelGenerationTool(SymbolCountInitialiser countInit,
                                  SpiralNoWinReelSetFactory noWinFactory,
                                  WinReelSetFactory winFactory,
                                  PaytableGenerator paytableGen,
                                  ReelSetWeightTuner weightTuner,
                                  RestrictionBuilder restrictionBuilder,
                                  LlmService llmService,
                                  ObjectMapper mapper) {
        this.countInit          = countInit;
        this.noWinFactory       = noWinFactory;
        this.winFactory         = winFactory;
        this.paytableGen        = paytableGen;
        this.weightTuner        = weightTuner;
        this.restrictionBuilder = restrictionBuilder;
        this.llmService         = llmService;
        this.mapper             = mapper;
    }

    @Override
    public boolean supports(String strategy) {
        return "LTR".equalsIgnoreCase(strategy);
    }

    @Override
    public GeneratedReels generate(AgentRequest request) {
        return generate(request, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public GeneratedReels generate(AgentRequest request, AgentContext context) {

        // ── Step 1: LLM Plan → initial params ──────────────────────────────
        Map<String, Object> params = new HashMap<>(
                request.parameters() != null ? request.parameters() : Map.of());

        if (llmService.isAvailable()) {
            try {
                if (context != null) context.getTrace().setStatusMessage("🤖 LLM planning initial parameters…");
                String planJson = llmService.plan(request, context);
                Map<String, Object> planned = mapper.readValue(planJson, new TypeReference<>() {});
                planned.forEach((k, v) -> params.putIfAbsent(k, v));
                if (!request.parameters().containsKey("symbols") && planned.containsKey("symbols"))
                    params.put("symbols", planned.get("symbols"));
                if (!request.parameters().containsKey("lines") && planned.containsKey("lines"))
                    params.put("lines", planned.get("lines"));
                log.info("[ltr] LLM plan applied: maxIterations={} symsPerReel={} targetVolatility={} winVecDecay={}",
                        params.get("maxIterations"), params.get("symsPerReel"),
                        params.get("targetVolatility"), params.get("winVecDecay"));
                if (context != null) context.getTrace().setInitialPlan(planned);
                params.put("__planned__", planned);
            } catch (Exception e) {
                log.warn("[ltr] LLM plan failed, using user params: {}", e.getMessage());
            }
        }

        // ── Step 2: Build initial state ──────────────────────────────────────
        if (context != null) context.getTrace().setStatusMessage("⚙️ Building reel strips…");
        MutableState state = MutableState.from(params, request);
        state.rebuild(countInit, noWinFactory, winFactory, paytableGen, weightTuner, request);

        // Apply LLM plan paytable + weights on top of formula-built state
        Map<String, Object> planned = (Map<String, Object>) params.get("__planned__");
        if (planned != null) {
            if (planned.containsKey("paytable")) {
                applyPaytablePatch(planned, state, request);
                log.info("[ltr] LLM plan paytable applied");
            }
            if (planned.containsKey("weights")) {
                Object raw = planned.get("weights");
                if (raw instanceof List<?> list && list.size() == state.weights.length) {
                    double[] w = new double[list.size()];
                    for (int i = 0; i < list.size(); i++) w[i] = ((Number) list.get(i)).doubleValue();
                    weightTuner.normalise(w);
                    state.weights = w;
                    log.info("[ltr] LLM plan weights applied ({} values)", w.length);
                } else {
                    log.warn("[ltr] LLM plan weights skipped — length mismatch or wrong type");
                }
            }
        }

        if (context != null) context.getTrace().setInitialState(buildStateMap(state));

        final int maxIterations = toInt(params.get("maxIterations"), 5);
        long spinsPerIter = toLong(params.get("spinsPerIter"), ReelSetWeightTuner.DEFAULT_SPINS);
        long seed         = toLong(params.get("seed"), 42L);

        log.info("[ltr] starting ReAct loop: maxIterations={} spinsPerIter={} " +
                 "screenWidth={} screenHeight={} minMatch={} symsPerReel={} volatility={} noWinSets={}",
                maxIterations, spinsPerIter,
                state.screenWidth, state.screenHeight, state.minMatch,
                state.symsPerReel, state.volatility, state.noWinSets.size());

        // ── Iteration 0: simulate initial plan result ────────────────────────
        if (context != null) context.getTrace().setStatusMessage("🎰 Simulating initial state (iteration 0)…");
        ReelSetWeightTuner.SimStats iter0Stats = weightTuner.simulate(
                state.allReelSets(), state.weights, state.paytable,
                state.symbols, state.lines,
                state.minMatch, state.screenWidth, state.screenHeight,
                spinsPerIter, seed);
        if (context != null)
            context.getTrace().addIterationLog(
                    iterLog(0, maxIterations, iter0Stats, state.symbols, false, null));

        double bestRtpDist   = Math.abs(iter0Stats.rtp() - request.targetRtp())
                             + Math.abs(iter0Stats.hitRate() - request.targetHitRate());
        double[] bestWeights = state.weights.clone();
        Map<Integer, Map<Integer, Double>> bestPaytable = deepCopyPaytable(state.paytable);
        ReelSetWeightTuner.SimStats bestStats = iter0Stats;

        boolean rtpOk0 = Math.abs(iter0Stats.rtp() - request.targetRtp()) <= request.rtpDelta();
        boolean hrOk0  = Math.abs(iter0Stats.hitRate() - request.targetHitRate()) <= request.hitRateDelta();
        if (rtpOk0 && hrOk0) {
            log.info("[ltr] converged at iteration 0");
            return buildResult(state, iter0Stats, true, request);
        }

        // ── Step 3: ReAct tuning loop ─────────────────────────────────────────
        boolean converged = false;

        for (int iter = 1; iter <= maxIterations; iter++) {
            if (context != null && context.isCancelled())
                throw new java.util.concurrent.CancellationException("Cancelled by user");

            if (context != null)
                context.getTrace().setStatusMessage(
                    String.format("🤖 Agent tuning iteration %d / %d…", iter, maxIterations));

            // Build tools for this iteration — they hold a reference to the shared state
            LtrTuningTools tools = new LtrTuningTools(
                    state, request,
                    weightTuner, countInit, noWinFactory, winFactory, paytableGen,
                    mapper, context,
                    spinsPerIter, seed + iter);

            ReelSetWeightTuner.SimStats iterStats = null;
            Map<String, Object> toolSummary = null;

            if (llmService.isAvailable()) {
                try {
                    TuningAgent agent = llmService.createTuningAgent(tools);

                    String situation = buildSituationPrompt(state, iter, maxIterations, request);
                    String agentSummary = agent.tune(situation);

                    log.info("[ltr] iter={} agent summary: {}", iter, agentSummary);

                    // The last runSimulation() call inside agent populated lastStats
                    iterStats = tools.getLastStats();

                    // Build a patch summary from tool calls for the trace
                    toolSummary = buildToolCallSummary(tools);

                    if (context != null) {
                        context.getTrace().setStatusMessage(
                            String.format("🎰 Post-agent simulation for iteration %d…", iter));
                    }

                } catch (Exception e) {
                    log.warn("[ltr] ReAct agent failed at iter={}: {}", iter, e.getMessage());
                    toolSummary = Map.of("error", e.getMessage() != null ? e.getMessage() : "unknown");
                }
            }

            // If agent didn't run a simulation or failed, run one now to get current stats
            if (iterStats == null) {
                iterStats = weightTuner.simulate(
                        state.allReelSets(), state.weights, state.paytable,
                        state.symbols, state.lines,
                        state.minMatch, state.screenWidth, state.screenHeight,
                        spinsPerIter, seed + iter);
            }

            double rtpDist = Math.abs(iterStats.rtp() - request.targetRtp())
                           + Math.abs(iterStats.hitRate() - request.targetHitRate());
            if (rtpDist < bestRtpDist) {
                bestRtpDist  = rtpDist;
                bestWeights  = state.weights.clone();
                bestPaytable = deepCopyPaytable(state.paytable);
                bestStats    = iterStats;
            }

            boolean rtpOk = Math.abs(iterStats.rtp() - request.targetRtp()) <= request.rtpDelta();
            boolean hrOk  = Math.abs(iterStats.hitRate() - request.targetHitRate()) <= request.hitRateDelta();

            log.info("[ltr] iter={}/{} rtp={} hr={} toolCalls={} converged={}/{}",
                    iter, maxIterations,
                    String.format("%.2f", iterStats.rtp()),
                    String.format("%.2f", iterStats.hitRate()),
                    tools.getToolCallCount(),
                    rtpOk, hrOk);

            if (context != null)
                context.getTrace().addIterationLog(
                        iterLog(iter, maxIterations, iterStats, state.symbols,
                                rtpOk && hrOk, toolSummary));

            if (rtpOk && hrOk) {
                converged = true;
                log.info("[ltr] converged at iter={}", iter);
                break;
            }
        }

        // ── Step 4: Build output using best weights/paytable ─────────────────
        state.weights  = bestWeights;
        state.paytable = bestPaytable;

        if (!converged) {
            log.warn("[ltr] did not converge — returning best result rtp={} hr={}",
                    String.format("%.2f", bestStats.rtp()),
                    String.format("%.2f", bestStats.hitRate()));
        }

        return buildResult(state, bestStats, converged, request);
    }

    // ── paytable patch (used for plan override) ───────────────────────────────

    @SuppressWarnings("unchecked")
    private void applyPaytablePatch(Map<String, Object> patch, MutableState state, AgentRequest request) {
        Object raw = patch.get("paytable");
        if (!(raw instanceof Map<?,?> ptPatch)) return;
        for (Map.Entry<?,?> e : ptPatch.entrySet()) {
            int symId;
            try { symId = Integer.parseInt(String.valueOf(e.getKey())); }
            catch (NumberFormatException ignored) { continue; }
            if (e.getValue() instanceof Map<?,?> symPatch) {
                Map<Integer, Double> existing = state.paytable.computeIfAbsent(
                        symId, k -> new LinkedHashMap<>());
                for (Map.Entry<?,?> pe : symPatch.entrySet()) {
                    int matchCount;
                    double mult;
                    try {
                        matchCount = Integer.parseInt(String.valueOf(pe.getKey()));
                        mult       = ((Number) pe.getValue()).doubleValue();
                    } catch (Exception ignored) { continue; }
                    existing.put(matchCount, mult);
                }
            }
        }
        if (request.maxPayout() > 0) paytableGen.capPaytable(state.paytable, request.maxPayout());
    }

    // ── MutableState ──────────────────────────────────────────────────────────

    public static class MutableState {
        public int screenWidth, screenHeight, minMatch, symsPerReel;
        public double winVecDecay;
        public String volatility;
        public List<SymbolDef> symbols;
        public List<int[]> lines;
        public double[] weights;
        public Map<Integer, Map<Integer, Double>> paytable;

        // built strips
        public List<int[][]> noWinSets;
        public List<WinReelSetFactory.WinReelEntry> winEntries;

        public List<int[][]> allReelSets() {
            List<int[][]> all = new ArrayList<>(noWinSets);
            winEntries.stream().map(WinReelSetFactory.WinReelEntry::reelSet).forEach(all::add);
            return all;
        }

        public List<int[][]> winSets() {
            return winEntries.stream().map(WinReelSetFactory.WinReelEntry::reelSet).toList();
        }

        @SuppressWarnings("unchecked")
        public static MutableState from(Map<String, Object> params, AgentRequest request) {
            MutableState s = new MutableState();
            s.screenWidth  = toInt(params.get("screenWidth"),  5);
            s.screenHeight = toInt(params.get("screenHeight"), 3);
            s.minMatch     = toInt(params.get("minMatch"),     3);
            s.symsPerReel  = toInt(params.get("symsPerReel"),  256);
            Object decayObj = params.get("winVecDecay");
            s.winVecDecay  = (decayObj instanceof Number n) ? n.doubleValue() : Double.NaN;
            s.volatility   = params.containsKey("targetVolatility")
                           ? (String) params.get("targetVolatility") : request.targetVolatility();
            s.symbols = parseSymbols((List<Map<String, Object>>) params.get("symbols"));
            s.lines   = parseLines((List<List<Integer>>) params.get("lines"));
            return s;
        }

        public void rebuild(SymbolCountInitialiser countInit,
                     SpiralNoWinReelSetFactory noWinFactory,
                     WinReelSetFactory winFactory,
                     PaytableGenerator paytableGen,
                     ReelSetWeightTuner weightTuner,
                     AgentRequest request) {
            int[] baseCounts = countInit.initialise(symbols, volatility, symsPerReel);
            noWinSets  = noWinFactory.create(baseCounts, screenWidth, minMatch);
            winEntries = Double.isNaN(winVecDecay)
                       ? winFactory.create(noWinSets, symbols, baseCounts, screenWidth, volatility)
                       : winFactory.createWithDecay(noWinSets, symbols, baseCounts, screenWidth, winVecDecay);
            paytable   = paytableGen.generate(symbols, minMatch, screenWidth, volatility, request.targetHitRate(), request.maxPayout());
            weights    = weightTuner.seedWeights(noWinSets, winSets(), symbols, request.targetRtp());
        }

        private static List<SymbolDef> parseSymbols(List<Map<String, Object>> raw) {
            if (raw == null) return Collections.emptyList();
            List<SymbolDef> list = new ArrayList<>();
            for (Map<String, Object> s : raw) {
                int id      = toInt(s.get("symbolId"), 0);
                String tier = (String) s.getOrDefault("tier", "junior");
                String hint = (String) s.getOrDefault("hint", "");
                list.add(new SymbolDef(id, tier, hint));
            }
            return list;
        }

        private static List<int[]> parseLines(List<List<Integer>> raw) {
            if (raw == null) return Collections.emptyList();
            List<int[]> lines = new ArrayList<>();
            for (List<Integer> row : raw)
                lines.add(row.stream().mapToInt(Integer::intValue).toArray());
            return lines;
        }

        public static int toInt(Object v, int def) {
            if (v instanceof Number n) return n.intValue();
            if (v instanceof String s) { try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
            return def;
        }
    }

    // ── prompt builder ────────────────────────────────────────────────────────

    private String buildSituationPrompt(MutableState state, int iter, int maxIter,
                                         AgentRequest request) {
        Map<String, Object> s = buildStateMap(state);
        String stateJson;
        try { stateJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(s); }
        catch (Exception e) { stateJson = s.toString(); }

        return String.format("""
            Iteration %d of %d.

            Targets: RTP=%.2f%%±%.2f%%  hitRate=%.2f%%±%.2f%%
            MaxPayout: %s

            Current state:
            %s

            Use getTargets() and getCurrentState() for full details.
            Call runSimulation() first to see current RTP, then adjust and verify.
            """,
            iter, maxIter,
            request.targetRtp(), request.rtpDelta(),
            request.targetHitRate(), request.hitRateDelta(),
            request.maxPayout() > 0 ? request.maxPayout() + "×" : "uncapped",
            stateJson);
    }

    // ── tool call summary for trace ───────────────────────────────────────────

    private Map<String, Object> buildToolCallSummary(LtrTuningTools tools) {
        List<LtrTuningTools.ToolCallRecord> calls = tools.getToolCallLog();
        if (calls.isEmpty()) return null;
        Map<String, Object> summary = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>();
        for (LtrTuningTools.ToolCallRecord call : calls) {
            if (!call.tool().startsWith("+")) keys.add(call.tool());
        }
        if (!keys.isEmpty()) summary.put("toolCalls", keys);
        summary.put("totalCalls", tools.getToolCallCount());
        return summary;
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private Map<String, Object> buildStateMap(MutableState state) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("symsPerReel",     state.symsPerReel);
        s.put("screenWidth",     state.screenWidth);
        s.put("screenHeight",    state.screenHeight);
        s.put("minMatch",        state.minMatch);
        s.put("targetVolatility",state.volatility);
        s.put("winVecDecay",     Double.isNaN(state.winVecDecay) ? "volatility-default" : state.winVecDecay);
        s.put("noWinSetCount",   state.noWinSets.size());
        s.put("winSetCount",     state.winEntries.size());
        List<Double> wList = new ArrayList<>();
        for (double w : state.weights) wList.add(Math.round(w * 10000.0) / 10000.0);
        s.put("weights", wList);
        Map<String, Object> pt = new LinkedHashMap<>();
        state.paytable.forEach((id, pays) -> pt.put(String.valueOf(id), pays));
        s.put("paytable", pt);
        return s;
    }

    private Map<String, Object> buildHitDistMap(ReelSetWeightTuner.SimStats stats, List<SymbolDef> symbols) {
        if (stats.hitDistribution() == null || stats.hitDistribution().isEmpty()) return null;
        Map<String, Object> dist = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Integer, Long>> e : stats.hitDistribution().entrySet()) {
            int symId = e.getKey();
            String label = symbols.stream()
                    .filter(s -> s.symbolId() == symId)
                    .findFirst()
                    .map(s -> s.hint() != null && !s.hint().isBlank() ? s.hint() : "sym-" + symId)
                    .orElse("sym-" + symId);
            Map<String, Object> byCount = new LinkedHashMap<>();
            for (Map.Entry<Integer, Long> ce : e.getValue().entrySet()) {
                long hits = ce.getValue();
                double pct = hits * 100.0 / stats.spins();
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("hits", hits);
                entry.put("hitRatePct", Math.round(pct * 10000.0) / 10000.0);
                byCount.put(ce.getKey() + "x", entry);
            }
            dist.put(label + " (id=" + symId + ")", byCount);
        }
        return dist;
    }

    private ExecutionTrace.IterationLog iterLog(int iter, int maxIterations,
                                                ReelSetWeightTuner.SimStats stats,
                                                List<SymbolDef> symbols,
                                                boolean converged,
                                                Map<String, Object> patch) {
        return new ExecutionTrace.IterationLog(
                iter, maxIterations,
                round2(stats.rtp()), round2(stats.hitRate()), round2(stats.stdDev()),
                converged, patch, buildHitDistMap(stats, symbols));
    }

    private String buildSimJson(ReelSetWeightTuner.SimStats stats, List<SymbolDef> symbols) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> sim  = new LinkedHashMap<>();
        sim.put("rtp",             round2(stats.rtp()));
        sim.put("hitRate",         round2(stats.hitRate()));
        sim.put("maxWin",          round2(stats.maxWin()));
        sim.put("volatilityIndex", round2(stats.volatilityIndex()));
        sim.put("volatilityLabel", stats.volatilityLabel());
        sim.put("spins",           stats.spins());
        Map<String, Object> dist = buildHitDistMap(stats, symbols);
        if (dist != null) sim.put("hitDistribution", dist);
        root.put("simulation", sim);
        try { return mapper.writeValueAsString(root); }
        catch (Exception e) { return "{}"; }
    }

    // ── result builder ────────────────────────────────────────────────────────

    private GeneratedReels buildResult(MutableState state,
                                        ReelSetWeightTuner.SimStats stats,
                                        boolean converged,
                                        AgentRequest request) {
        List<Restriction> restrictions = new ArrayList<>();
        double targetHr = request.targetHitRate();
        for (int i = 0; i < state.noWinSets.size(); i++)
            restrictions.add(restrictionBuilder.build(state.screenHeight, targetHr, false));
        for (int i = 0; i < state.winEntries.size(); i++)
            restrictions.add(restrictionBuilder.build(state.screenHeight, targetHr, true));

        List<ReelSet> reelSets = new ArrayList<>();
        List<int[][]> all = state.allReelSets();
        for (int i = 0; i < all.size(); i++) {
            int[][] rs = all.get(i);
            List<List<Integer>> tilesCounts = new ArrayList<>();
            for (int[] reel : rs) {
                List<Integer> reelList = new ArrayList<>();
                for (int c : reel) reelList.add(c);
                tilesCounts.add(reelList);
            }
            reelSets.add(new ReelSet(tilesCounts, List.of(restrictions.get(i))));
        }
        ReelSetsCollectionData config = new ReelSetsCollectionData(Strategy.SHUFFLE, reelSets);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("converged", converged);

        Map<String, Object> sim = new LinkedHashMap<>();
        sim.put("rtp",             round2(stats.rtp()));
        sim.put("hitRate",         round2(stats.hitRate()));
        sim.put("maxWin",          round2(stats.maxWin()));
        sim.put("stdDev",          round2(stats.stdDev()));
        sim.put("volatilityIndex", round2(stats.volatilityIndex()));
        sim.put("volatilityLabel", stats.volatilityLabel());
        sim.put("spins",           stats.spins());
        Map<String, Object> dist = buildHitDistMap(stats, state.symbols);
        if (dist != null && !dist.isEmpty()) sim.put("hitDistribution", dist);
        response.put("simulation", sim);
        response.put("paytable", state.paytable);

        List<Double> wList = new ArrayList<>();
        for (double w : state.weights) wList.add(round2(w));
        response.put("weights", wList);

        List<Map<String, Object>> reelSetInfos = new ArrayList<>();
        int winOffset = state.noWinSets.size();
        for (int i = 0; i < all.size(); i++) {
            Map<String, Object> info = new LinkedHashMap<>();
            String name;
            if (i < winOffset) {
                name = i + ": no-win";
            } else {
                WinReelSetFactory.WinReelEntry entry = state.winEntries.get(i - winOffset);
                SymbolDef sym = entry.symbol();
                String hint = sym.hint() != null && !sym.hint().isBlank() ? sym.hint() : "sym-" + sym.symbolId();
                name = i + ": " + hint + " (" + sym.tier() + ")";
            }
            info.put("name", name);
            info.put("tilesCounts", reelSets.get(i).tilesCounts());
            Restriction r = restrictions.get(i);
            info.put("restriction", Map.of(
                    "stackSizes", r.stacks(),
                    "stackChances", r.chances(),
                    "minDistance", r.distance()));
            reelSetInfos.add(info);
        }
        response.put("reelSets", reelSetInfos);

        try {
            String shuffledJson = ShuffleGenerator.generateStackedReels(config);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parsedSets = mapper.readValue(shuffledJson, List.class);
            for (int i = 0; i < reelSetInfos.size() && i < parsedSets.size(); i++)
                reelSetInfos.get(i).put("reelStrips", parsedSets.get(i).get("reelSet"));
        } catch (Exception e) {
            log.warn("[ltr] ShuffleGenerator failed, reelStrips omitted: {}", e.getMessage());
        }

        try {
            return new GeneratedReels(config, mapper.writeValueAsString(response));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise LTR result", e);
        }
    }

    // ── utilities ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<Integer, Map<Integer, Double>> deepCopyPaytable(
            Map<Integer, Map<Integer, Double>> src) {
        Map<Integer, Map<Integer, Double>> copy = new LinkedHashMap<>();
        src.forEach((symId, pays) -> copy.put(symId, new LinkedHashMap<>(pays)));
        return copy;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static int toInt(Object v, int def) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) { try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
        return def;
    }

    private static long toLong(Object v, long def) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) { try { return Long.parseLong(s); } catch (NumberFormatException ignored) {} }
        return def;
    }
}
