package com.slotslab.simulation.eval;

import com.slotslab.dto.ways.WayLineDto;
import com.slotslab.dto.ways.WayLinesDto;
import com.slotslab.simulation.config.SymbolConfig;
import com.slotslab.simulation.config.SymbolTable;
import com.slotslab.simulation.config.SymbolType;
import com.slotslab.simulation.config.WildMultiplierAggregation;
import com.slotslab.simulation.stats.ComboKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public final class WaysEvaluator {

    private WaysEvaluator() {}

    public static WayLinesDto evalWays(int[][] screen, int reelCount, int screenHeight,
                                SymbolTable symbols, int minMatch) {
        List<WayLineDto> lines = new ArrayList<>();
        int floatId = 0;
        boolean wildOnlyClaimed = false;

        TreeMap<Integer, SymbolConfig> normals = new TreeMap<>();
        for (SymbolConfig sc : symbols.all()) {
            if (sc.type() == SymbolType.NORMAL) normals.put(sc.symbolId(), sc);
        }

        for (var entry : normals.entrySet()) {
            int sym = entry.getKey();
            SymbolConfig cfg = entry.getValue();

            List<List<Integer>> line2DimPos = new ArrayList<>();
            boolean broke = false;
            for (int r = 0; r < reelCount; r++) {
                List<Integer> posPerReel = getPositions(screen, r, screenHeight, sym, symbols);
                if (posPerReel.isEmpty()) { broke = true; break; }
                line2DimPos.add(posPerReel);
            }
            if (broke && line2DimPos.size() < minMatch) continue;
            if (line2DimPos.size() < minMatch) continue;
            if (!containsAtLeastOneNormal(line2DimPos, screen, sym, symbols)) continue;

            wildOnlyClaimed = true;

            int lineSize = line2DimPos.size();
            if (cfg.paytable() == null || cfg.paytable().isEmpty()) continue;
            int ptIdx = lineSize - minMatch;
            if (ptIdx < 0 || ptIdx >= cfg.paytable().size()) continue;
            double singularPay = cfg.paytable().get(ptIdx);
            if (singularPay <= 0) continue;

            List<List<Integer>> line2DimSym = extract2DimSym(line2DimPos, screen);
            List<Integer> ways = extractWays(line2DimPos);
            List<Integer> waysWithMult = extractWaysWithMultipliers(ways, line2DimSym, symbols);

            double totalWaysMult = 1.0;
            for (int w : waysWithMult) totalWaysMult *= w;

            double winAmount = Math.round(singularPay * totalWaysMult * 100.0) / 100.0;
            if (winAmount <= 0) continue;

            List<Integer> line1DimPos = extract1DimPos(line2DimPos, reelCount);
            List<Integer> line1DimSym = extract1DimSym(line1DimPos, screen, reelCount);
            int totalSimpleLines = waysWithMult.stream().reduce(1, (a, b) -> a * b);

            lines.add(new WayLineDto(
                    floatId++, lineSize, 0, sym,
                    Math.round(singularPay * 100.0) / 100.0,
                    winAmount,
                    line1DimPos, line1DimSym,
                    line2DimPos, line2DimSym,
                    ways, waysWithMult, totalSimpleLines
            ));
        }

        // wild-only streak: fires only when wilds could not connect to any normal symbol
        if (!wildOnlyClaimed) {
            WayLineDto wildOnlyLine = evalWildOnlyWays(screen, reelCount, screenHeight, symbols, minMatch, floatId, normals);
            if (wildOnlyLine != null) lines.add(wildOnlyLine);
        }

        if (lines.isEmpty()) return null;
        return WayLinesDto.of(lines, 1.0);
    }

    public static double evalWaysTracked(int[][] screen, int reelCount, int screenHeight,
                                  SymbolTable symbols, int minMatch,
                                  Map<ComboKey, long[]> hitMap,
                                  Map<ComboKey, double[]> payMap) {
        double total = 0.0;
        boolean wildOnlyClaimed = false;

        TreeMap<Integer, SymbolConfig> normals = new TreeMap<>();
        for (SymbolConfig sc : symbols.all()) {
            if (sc.type() == SymbolType.NORMAL) normals.put(sc.symbolId(), sc);
        }

        for (var entry : normals.entrySet()) {
            int sym = entry.getKey();
            SymbolConfig cfg = entry.getValue();

            List<List<Integer>> line2DimPos = new ArrayList<>();
            boolean broke = false;
            for (int r = 0; r < reelCount; r++) {
                List<Integer> pos = getPositions(screen, r, screenHeight, sym, symbols);
                if (pos.isEmpty()) { broke = true; break; }
                line2DimPos.add(pos);
            }
            if (broke && line2DimPos.size() < minMatch) continue;
            if (line2DimPos.size() < minMatch) continue;
            if (!containsAtLeastOneNormal(line2DimPos, screen, sym, symbols)) continue;

            wildOnlyClaimed = true;

            int lineSize = line2DimPos.size();
            if (cfg.paytable() == null || cfg.paytable().isEmpty()) continue;
            int ptIdx = lineSize - minMatch;
            if (ptIdx < 0 || ptIdx >= cfg.paytable().size()) continue;
            double singularPay = cfg.paytable().get(ptIdx);
            if (singularPay <= 0) continue;

            List<List<Integer>> line2DimSym = extract2DimSym(line2DimPos, screen);
            List<Integer> ways = extractWays(line2DimPos);
            List<Integer> waysWithMult = extractWaysWithMultipliers(ways, line2DimSym, symbols);

            double totalWaysMult = 1.0;
            for (int w : waysWithMult) totalWaysMult *= w;

            double win = Math.round(singularPay * totalWaysMult * 100.0) / 100.0;
            if (win <= 0) continue;

            total += win;
            int totalSimpleLines = waysWithMult.stream().reduce(1, (a, b) -> a * b);
            ComboKey key = new ComboKey(sym, lineSize);
            hitMap.computeIfAbsent(key, k -> new long[1])[0] += totalSimpleLines;
            payMap.computeIfAbsent(key, k -> new double[1])[0] += win;
        }

        // wild-only streak: only when no normal symbol was present on any qualifying reel
        if (!wildOnlyClaimed) {
            double wildOnlyWin = evalWildOnlyWaysTracked(screen, reelCount, screenHeight, symbols, minMatch, normals, hitMap, payMap);
            total += wildOnlyWin;
        }

        return total;
    }

    // ── wild-only ways ────────────────────────────────────────────────────────

    private record WildOnlyResult(
            List<List<Integer>> wildPos, List<List<Integer>> line2DimSym,
            List<Integer> ways, List<Integer> waysWithMult,
            int lineSize, int totalSimpleLines,
            double singularPay, double winAmount) {}

    private static WildOnlyResult computeWildOnly(int[][] screen, int reelCount, int screenHeight,
                                                   SymbolTable symbols, int minMatch,
                                                   TreeMap<Integer, SymbolConfig> normals) {
        int wildId = symbols.wildId();
        if (wildId < 0) return null;

        List<List<Integer>> wildPos = new ArrayList<>();
        boolean broke = false;
        for (int r = 0; r < reelCount; r++) {
            List<Integer> pos = getWildOnlyPositions(screen, r, screenHeight, wildId, symbols);
            if (pos.isEmpty()) { broke = true; break; }
            wildPos.add(pos);
        }
        if ((broke || wildPos.size() < reelCount) && wildPos.size() < minMatch) return null;
        if (wildPos.size() < minMatch) return null;

        for (int r = 0; r < wildPos.size(); r++) {
            for (int row : wildPos.get(r)) {
                if (!symbols.isWild(screen[r][row])) return null;
            }
        }

        int lineSize = wildPos.size();
        double singularPay = bestPay(lineSize, minMatch, normals);
        if (singularPay <= 0) return null;

        List<List<Integer>> line2DimSym = extract2DimSym(wildPos, screen);
        List<Integer> ways = extractWays(wildPos);
        List<Integer> waysWithMult = extractWaysWithMultipliers(ways, line2DimSym, symbols);

        double totalWaysMult = 1.0;
        for (int w : waysWithMult) totalWaysMult *= w;

        double winAmount = Math.round(singularPay * totalWaysMult * 100.0) / 100.0;
        if (winAmount <= 0) return null;

        int totalSimpleLines = waysWithMult.stream().reduce(1, (a, b) -> a * b);
        return new WildOnlyResult(wildPos, line2DimSym, ways, waysWithMult,
                lineSize, totalSimpleLines, singularPay, winAmount);
    }

    private static WayLineDto evalWildOnlyWays(int[][] screen, int reelCount, int screenHeight,
                                               SymbolTable symbols, int minMatch, int floatId,
                                               TreeMap<Integer, SymbolConfig> normals) {
        WildOnlyResult r = computeWildOnly(screen, reelCount, screenHeight, symbols, minMatch, normals);
        if (r == null) return null;

        int wildId = symbols.wildId();
        List<Integer> line1DimPos = extract1DimPos(r.wildPos(), reelCount);
        List<Integer> line1DimSym = extract1DimSym(line1DimPos, screen, reelCount);

        return new WayLineDto(
                floatId, r.lineSize(), 0, wildId,
                Math.round(r.singularPay() * 100.0) / 100.0,
                r.winAmount(),
                line1DimPos, line1DimSym,
                r.wildPos(), r.line2DimSym(),
                r.ways(), r.waysWithMult(), r.totalSimpleLines()
        );
    }

    private static double evalWildOnlyWaysTracked(int[][] screen, int reelCount, int screenHeight,
                                                  SymbolTable symbols, int minMatch,
                                                  TreeMap<Integer, SymbolConfig> normals,
                                                  Map<ComboKey, long[]> hitMap,
                                                  Map<ComboKey, double[]> payMap) {
        WildOnlyResult r = computeWildOnly(screen, reelCount, screenHeight, symbols, minMatch, normals);
        if (r == null) return 0.0;

        int wildId = symbols.wildId();
        ComboKey key = new ComboKey(wildId, r.lineSize());
        hitMap.computeIfAbsent(key, k -> new long[1])[0] += r.totalSimpleLines();
        payMap.computeIfAbsent(key, k -> new double[1])[0] += r.winAmount();
        return r.winAmount();
    }

    private static double bestPay(int lineSize, int minMatch,
                                   TreeMap<Integer, SymbolConfig> normals) {
        int ptIdx = lineSize - minMatch;
        double best = 0.0;
        for (SymbolConfig sc : normals.values()) {
            if (sc.paytable() == null || ptIdx >= sc.paytable().size()) continue;
            double v = sc.paytable().get(ptIdx);
            if (v > best) best = v;
        }
        return best;
    }

    private static List<Integer> getWildOnlyPositions(int[][] screen, int reel, int screenHeight,
                                                      int wildId, SymbolTable symbols) {
        List<Integer> pos = new ArrayList<>();
        for (int row = 0; row < screenHeight; row++) {
            int s = screen[reel][row];
            if (s == 0) continue; // mask symbol
            if (symbols.isScatter(s)) continue;
            if (symbols.isWild(s)) pos.add(row);
        }
        return pos;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static List<Integer> getPositions(int[][] screen, int reel, int screenHeight,
                                              int targetSym, SymbolTable symbols) {
        List<Integer> pos = new ArrayList<>();
        for (int row = 0; row < screenHeight; row++) {
            int s = screen[reel][row];
            if (s == 0) continue; // mask symbol — non-existent position
            if (symbols.isScatter(s)) continue;
            if (s == targetSym || symbols.isWild(s)) pos.add(row);
        }
        return pos;
    }

    private static boolean containsAtLeastOneNormal(List<List<Integer>> line2DimPos,
                                                    int[][] screen, int sym,
                                                    SymbolTable symbols) {
        for (int r = 0; r < line2DimPos.size(); r++) {
            for (int row : line2DimPos.get(r)) {
                if (screen[r][row] == sym) return true;
            }
        }
        return false;
    }

    private static List<List<Integer>> extract2DimSym(List<List<Integer>> line2DimPos,
                                                      int[][] screen) {
        List<List<Integer>> result = new ArrayList<>(line2DimPos.size());
        for (int r = 0; r < line2DimPos.size(); r++) {
            List<Integer> syms = new ArrayList<>();
            for (int row : line2DimPos.get(r)) syms.add(screen[r][row]);
            result.add(syms);
        }
        return result;
    }

    private static List<Integer> extractWays(List<List<Integer>> line2DimPos) {
        List<Integer> ways = new ArrayList<>(line2DimPos.size());
        for (List<Integer> reel : line2DimPos) ways.add(reel.size());
        return ways;
    }

    private static List<Integer> extractWaysWithMultipliers(List<Integer> ways,
                                                            List<List<Integer>> line2DimSym,
                                                            SymbolTable symbols) {
        List<Integer> result = new ArrayList<>(ways);
        int wildId = symbols.wildId();
        if (wildId < 0) return result;
        SymbolConfig wildCfg = symbols.get(wildId);
        if (wildCfg == null || wildCfg.wildAggregation() != WildMultiplierAggregation.ADD) return result;

        for (int i = 0; i < result.size(); i++) {
            int extra = 0;
            for (int s : line2DimSym.get(i)) {
                if (s == wildId) {
                    int mult = (int) Math.floor(wildCfg.wildMultiplier());
                    if (mult > 1) extra += (mult - 1);
                }
            }
            result.set(i, ways.get(i) + extra);
        }
        return result;
    }

    private static List<Integer> extract1DimPos(List<List<Integer>> line2DimPos, int reelCount) {
        TreeSet<Integer> set = new TreeSet<>();
        for (int r = 0; r < line2DimPos.size(); r++) {
            for (int row : line2DimPos.get(r)) set.add(reelCount * row + r);
        }
        return new ArrayList<>(set);
    }

    private static List<Integer> extract1DimSym(List<Integer> line1DimPos,
                                                int[][] screen, int reelCount) {
        List<Integer> syms = new ArrayList<>(line1DimPos.size());
        for (int coded : line1DimPos) {
            int reel = coded % reelCount;
            int row  = coded / reelCount;
            syms.add(screen[reel][row]);
        }
        return syms;
    }
}
