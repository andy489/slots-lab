package com.slotslab.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
public class LtrReelGenerationTool implements AgentReelGenerationTool {

    private static final Logger log = LoggerFactory.getLogger(LtrReelGenerationTool.class);

    private final SymbolCountInitialiser countInit;
    private final SpiralNoWinReelSetFactory noWinFactory;
    private final WinReelSetFactory winFactory;
    private final PaytableGenerator paytableGen;
    private final ReelSetWeightTuner weightTuner;
    private final RestrictionBuilder restrictionBuilder;
    private final ObjectMapper mapper;

    public LtrReelGenerationTool(SymbolCountInitialiser countInit,
                                  SpiralNoWinReelSetFactory noWinFactory,
                                  WinReelSetFactory winFactory,
                                  PaytableGenerator paytableGen,
                                  ReelSetWeightTuner weightTuner,
                                  RestrictionBuilder restrictionBuilder,
                                  ObjectMapper mapper) {
        this.countInit  = countInit;
        this.noWinFactory = noWinFactory;
        this.winFactory   = winFactory;
        this.paytableGen  = paytableGen;
        this.weightTuner  = weightTuner;
        this.restrictionBuilder = restrictionBuilder;
        this.mapper = mapper;
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
        Map<String, Object> params = request.parameters();

        // --- parse parameters ---
        int screenWidth      = toInt(params.get("screenWidth"), 5);
        int screenHeight     = toInt(params.get("screenHeight"), 3);
        int minMatch         = toInt(params.get("minMatch"), 3);
        int symsPerReel      = toInt(params.get("symsPerReel"), 256);
        int maxIterations    = toInt(params.get("maxIterations"), 80);
        String volatility    = (String) params.getOrDefault("targetVolatility",
                                        request.targetVolatility());

        List<Map<String, Object>> rawSymbols =
                (List<Map<String, Object>>) params.get("symbols");
        List<SymbolDef> symbols = parseSymbols(rawSymbols);

        List<List<Integer>> rawLines = (List<List<Integer>>) params.get("lines");
        List<int[]> lines = parseLines(rawLines);

        log.info("LTR generation: symbols={} screenWidth={} screenHeight={} minMatch={} symsPerReel={} volatility={}",
                symbols.size(), screenWidth, screenHeight, minMatch, symsPerReel, volatility);

        // --- Skill 1: base counts ---
        int[] baseCounts = countInit.initialise(symbols, volatility, symsPerReel);

        // --- Skill 2: no-win reel sets ---
        List<int[][]> noWinSets = noWinFactory.create(baseCounts, screenWidth, minMatch);

        // --- Skill 3: winning reel sets ---
        List<WinReelSetFactory.WinReelEntry> winEntries = winFactory.create(noWinSets, symbols, baseCounts, screenWidth, volatility);
        List<int[][]> winSets = winEntries.stream().map(WinReelSetFactory.WinReelEntry::reelSet).toList();

        // --- Skill 4: paytable ---
        Map<Integer, Map<Integer, Double>> paytable =
                paytableGen.generate(symbols, minMatch, screenWidth, volatility);

        // --- Skill 5: weight tuning ---
        WeightedReelSets weighted = weightTuner.tune(
                noWinSets, winSets, paytable, symbols, lines,
                minMatch, screenWidth, screenHeight,
                request.targetRtp(), request.rtpDelta(),
                request.targetHitRate(), request.hitRateDelta(),
                maxIterations,
                context != null ? context::isCancelled : () -> false);

        // --- Build ReelSetsCollectionData with SHUFFLE strategy ---
        // Each reel set gets its own restriction from RestrictionBuilder (skill 06):
        // no-win sets shift stack mass left for lower hit rate; win sets always bias right.
        List<Restriction> restrictions = new ArrayList<>();
        for (int i = 0; i < noWinSets.size(); i++) {
            restrictions.add(restrictionBuilder.build(screenHeight, request.targetHitRate(), false));
        }
        for (WinReelSetFactory.WinReelEntry ignored : winEntries) {
            restrictions.add(restrictionBuilder.build(screenHeight, request.targetHitRate(), true));
        }

        List<ReelSet> reelSets = new ArrayList<>();
        for (int i = 0; i < weighted.reelSets().size(); i++) {
            int[][] rs = weighted.reelSets().get(i);
            List<List<Integer>> tilesCounts = new ArrayList<>();
            for (int[] reel : rs) {
                List<Integer> reelList = new ArrayList<>();
                for (int c : reel) reelList.add(c);
                tilesCounts.add(reelList);
            }
            reelSets.add(new ReelSet(tilesCounts, List.of(restrictions.get(i))));
        }

        ReelSetsCollectionData config = new ReelSetsCollectionData(Strategy.SHUFFLE, reelSets);

        // --- Build response JSON with weights, paytable, convergence, simulation stats, and reel strips ---
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("converged", weighted.converged());

        // simulation statistics from final verification pass
        WeightedReelSets.SimStats ss = weighted.simStats();
        if (ss != null) {
            Map<String, Object> sim = new LinkedHashMap<>();
            sim.put("rtp",              ss.rtp());
            sim.put("hitRate",          ss.hitRate());
            sim.put("maxWin",           ss.maxWin());
            sim.put("stdDev",           ss.stdDev());
            sim.put("volatilityIndex",  ss.volatilityIndex());
            sim.put("volatilityLabel",  ss.volatilityLabel());
            sim.put("spins",            ss.spins());
            response.put("simulation", sim);
        }

        response.put("paytable", paytable);

        // top-level weights array — index matches reelSets array
        double[] rawWeights = weighted.weights();
        List<Double> weightsList = new ArrayList<>();
        for (double w : rawWeights) weightsList.add(Math.round(w * 10000.0) / 10000.0);
        response.put("weights", weightsList);

        List<Map<String, Object>> reelSetInfos = new ArrayList<>();
        int winOffset = noWinSets.size();
        for (int i = 0; i < weighted.reelSets().size(); i++) {
            Map<String, Object> info = new LinkedHashMap<>();
            String name;
            if (i < winOffset) {
                name = i + ": no-win";
            } else {
                WinReelSetFactory.WinReelEntry entry = winEntries.get(i - winOffset);
                SymbolDef sym = entry.symbol();
                String hint = sym.hint() != null && !sym.hint().isBlank() ? sym.hint() : "sym-" + sym.symbolId();
                name = i + ": " + hint + " (" + sym.tier() + ")";
            }
            info.put("name", name);
            info.put("tilesCounts", reelSets.get(i).tilesCounts());
            Restriction r = restrictions.get(i);
            Map<String, Object> rInfo = new LinkedHashMap<>();
            rInfo.put("stackSizes", r.stacks());
            rInfo.put("stackChances", r.chances());
            rInfo.put("minDistance", r.distance());
            info.put("restriction", rInfo);
            reelSetInfos.add(info);
        }
        response.put("reelSets", reelSetInfos);

        // generate shuffled reel strips and attach to each reel set entry
        try {
            String shuffledJson = ShuffleGenerator.generateStackedReels(config);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parsedSets = mapper.readValue(shuffledJson, List.class);
            for (int i = 0; i < reelSetInfos.size() && i < parsedSets.size(); i++) {
                reelSetInfos.get(i).put("reelStrips", parsedSets.get(i).get("reelSet"));
            }
        } catch (Exception e) {
            log.warn("ShuffleGenerator failed, reelStrips omitted: {}", e.getMessage());
        }

        try {
            String json = mapper.writeValueAsString(response);
            return new GeneratedReels(config, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise LTR generation result", e);
        }
    }

    private List<SymbolDef> parseSymbols(List<Map<String, Object>> raw) {
        if (raw == null) return Collections.emptyList();
        List<SymbolDef> list = new ArrayList<>();
        for (Map<String, Object> s : raw) {
            int id     = toInt(s.get("symbolId"), 0);
            String tier  = (String) s.getOrDefault("tier", "junior");
            String hint  = (String) s.getOrDefault("hint", "");
            list.add(new SymbolDef(id, tier, hint));
        }
        return list;
    }

    private List<int[]> parseLines(List<List<Integer>> raw) {
        if (raw == null) return Collections.emptyList();
        List<int[]> lines = new ArrayList<>();
        for (List<Integer> row : raw) {
            lines.add(row.stream().mapToInt(Integer::intValue).toArray());
        }
        return lines;
    }

    private int toInt(Object v, int def) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) { try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
        return def;
    }
}
