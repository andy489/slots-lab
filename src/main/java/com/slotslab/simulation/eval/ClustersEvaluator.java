package com.slotslab.simulation.eval;

import com.slotslab.dto.scatters.ContactDto;
import com.slotslab.dto.scatters.ContactsDto;
import com.slotslab.simulation.config.AdjacencyOffset;
import com.slotslab.simulation.config.ScattersIntervalSet;
import com.slotslab.simulation.config.ScattersPaytableEntry;
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

/**
 * Clusters Pay: symbols pay when they form a connected cluster of ≥ minMatch tiles.
 * Connectivity is defined by the supplied adjacency offsets (e.g. 4-direction or 8-direction).
 * Reuses the interval-based paytable from ScattersIntervalSet.
 */
public final class ClustersEvaluator {

    private static final int VISITED = -1;

    private ClustersEvaluator() {}

    // ── public API ────────────────────────────────────────────────────────────

    public static ContactsDto evalClusters(int[][] screen, int reelCount, int screenHeight,
                                           SymbolTable symbols, int minMatch,
                                           List<ScattersIntervalSet> intervalSets,
                                           List<AdjacencyOffset> adjacency) {
        List<ContactDto> contacts = new ArrayList<>();
        int floatId = 0;

        TreeMap<Integer, SymbolConfig> normals = normals(symbols);
        int[][] offsets = toArray(adjacency);

        for (var entry : normals.entrySet()) {
            int sym = entry.getKey();
            SymbolConfig symCfg = entry.getValue();
            List<ScattersPaytableEntry> paytable = ScattersEvaluator.resolveSet(intervalSets, symCfg.contactsIntervalSetName());

            // work on a copy so DFS can mark visited
            int[][] copy = copyScreen(screen, reelCount, screenHeight);

            for (int r = 0; r < reelCount; r++) {
                for (int row = 0; row < screenHeight; row++) {
                    if (copy[r][row] != sym) continue;

                    // ── DFS cluster detection ─────────────────────────────────
                    List<List<Integer>> pos2D = new ArrayList<>();
                    for (int i = 0; i < reelCount; i++) pos2D.add(new ArrayList<>());
                    int[] sizeAndWild = {0, 0}; // [clusterSize, wildCount]
                    double[] wildMult = {1.0};

                    dfs(r, row, copy, sym, offsets, reelCount, screenHeight,
                            symbols, pos2D, sizeAndWild, wildMult);

                    int clusterSize = sizeAndWild[0];
                    int wildCount   = sizeAndWild[1];
                    if (clusterSize < minMatch) continue;

                    // ── paytable lookup ───────────────────────────────────────
                    int intervalIdx = ScattersEvaluator.lookupIntervalIndex(paytable, clusterSize);
                    if (intervalIdx < 0) continue;

                    List<Double> symPaytable = symCfg.paytable();
                    if (symPaytable == null || intervalIdx >= symPaytable.size()) continue;
                    double singularPay = symPaytable.get(intervalIdx);
                    if (singularPay <= 0) continue;

                    double localMultiplier = wildCount > 0 ? wildMult[0] : 1.0;
                    double win = Math.round(singularPay * localMultiplier * 100.0) / 100.0;
                    if (win <= 0) continue;

                    // sort row positions per reel
                    for (List<Integer> rp : pos2D) rp.sort(null);

                    List<Integer> contact1DimPos = extract1DimPos(pos2D, reelCount);
                    List<List<Integer>> contact2DimSym = extract2DimSym(pos2D, screen);
                    List<Integer> contact1DimSym = extract1DimSym(contact2DimSym);
                    int[] start = extractStart(pos2D);

                    contacts.add(new ContactDto(
                            floatId++, clusterSize, sym, start[0], start[1],
                            localMultiplier,
                            Math.round(singularPay * 100.0) / 100.0,
                            win,
                            contact1DimPos, contact1DimSym,
                            pos2D, contact2DimSym
                    ));
                }
            }
        }

        if (contacts.isEmpty()) return null;
        return ContactsDto.of(contacts, 1.0);
    }

    public static double evalClustersTracked(int[][] screen, int reelCount, int screenHeight,
                                              SymbolTable symbols, int minMatch,
                                              List<ScattersIntervalSet> intervalSets,
                                              List<AdjacencyOffset> adjacency,
                                              Map<ComboKey, long[]> hitMap,
                                              Map<ComboKey, double[]> payMap) {
        double total = 0.0;

        TreeMap<Integer, SymbolConfig> normals = normals(symbols);
        int[][] offsets = toArray(adjacency);

        for (var entry : normals.entrySet()) {
            int sym = entry.getKey();
            SymbolConfig symCfg = entry.getValue();
            List<ScattersPaytableEntry> paytable = ScattersEvaluator.resolveSet(intervalSets, symCfg.contactsIntervalSetName());

            int[][] copy = copyScreen(screen, reelCount, screenHeight);

            for (int r = 0; r < reelCount; r++) {
                for (int row = 0; row < screenHeight; row++) {
                    if (copy[r][row] != sym) continue;

                    List<List<Integer>> pos2D = new ArrayList<>();
                    for (int i = 0; i < reelCount; i++) pos2D.add(new ArrayList<>());
                    int[] sizeAndWild = {0, 0};
                    double[] wildMult = {1.0};

                    dfs(r, row, copy, sym, offsets, reelCount, screenHeight,
                            symbols, pos2D, sizeAndWild, wildMult);

                    int clusterSize = sizeAndWild[0];
                    int wildCount   = sizeAndWild[1];
                    if (clusterSize < minMatch) continue;

                    int intervalIdx = ScattersEvaluator.lookupIntervalIndex(paytable, clusterSize);
                    if (intervalIdx < 0) continue;

                    List<Double> symPaytable = symCfg.paytable();
                    if (symPaytable == null || intervalIdx >= symPaytable.size()) continue;
                    double singularPay = symPaytable.get(intervalIdx);
                    if (singularPay <= 0) continue;

                    double localMultiplier = wildCount > 0 ? wildMult[0] : 1.0;
                    double win = Math.round(singularPay * localMultiplier * 100.0) / 100.0;
                    if (win <= 0) continue;

                    total += win;
                    ComboKey key = new ComboKey(sym, intervalIdx);
                    hitMap.computeIfAbsent(key, k -> new long[1])[0]++;
                    payMap.computeIfAbsent(key, k -> new double[1])[0] += win;
                }
            }
        }

        return total;
    }

    // ── DFS ──────────────────────────────────────────────────────────────────

    private static void dfs(int reel, int row, int[][] screen,
                             int trackedSym, int[][] offsets,
                             int reelCount, int screenHeight,
                             SymbolTable symbols,
                             List<List<Integer>> pos2D,
                             int[] sizeAndWild,
                             double[] wildMult) {
        screen[reel][row] = VISITED;
        sizeAndWild[0]++;
        pos2D.get(reel).add(row);

        for (int[] off : offsets) {
            int nr = reel + off[0];
            int nw = row  + off[1];
            if (nr < 0 || nw < 0 || nr >= reelCount || nw >= screenHeight) continue;
            int tile = screen[nr][nw];
            if (tile == trackedSym) {
                dfs(nr, nw, screen, trackedSym, offsets, reelCount, screenHeight,
                        symbols, pos2D, sizeAndWild, wildMult);
            } else if (symbols.isWild(tile)) {
                // accumulate wild multiplier and count it in cluster
                SymbolConfig wc = symbols.get(tile);
                wildMult[0] = accumulateWild(wc, wildMult[0], sizeAndWild[1]);
                sizeAndWild[1]++;
                screen[nr][nw] = VISITED;
                sizeAndWild[0]++;
                pos2D.get(nr).add(nw);
                // continue DFS from wild position
                for (int[] off2 : offsets) {
                    int nr2 = nr + off2[0];
                    int nw2 = nw + off2[1];
                    if (nr2 < 0 || nw2 < 0 || nr2 >= reelCount || nw2 >= screenHeight) continue;
                    int tile2 = screen[nr2][nw2];
                    if (tile2 == trackedSym) {
                        dfs(nr2, nw2, screen, trackedSym, offsets, reelCount, screenHeight,
                                symbols, pos2D, sizeAndWild, wildMult);
                    }
                }
            }
        }
    }

    private static double accumulateWild(SymbolConfig wildCfg, double current, int wildCount) {
        if (wildCfg == null || wildCfg.wildAggregation() == WildMultiplierAggregation.NONE) {
            return current;
        }
        double m = wildCfg.wildMultiplier();
        return switch (wildCfg.wildAggregation()) {
            case ADD      -> wildCount == 0 ? m : current + m;
            case MULTIPLY -> wildCount == 0 ? m : current * m;
            case SEQUENCE -> {
                List<Double> seq = wildCfg.wildSequence();
                yield (seq != null && wildCount < seq.size()) ? seq.get(wildCount) : current;
            }
            default -> current;
        };
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static int[][] toArray(List<AdjacencyOffset> adjacency) {
        if (adjacency == null || adjacency.isEmpty()) {
            // default: 4-direction
            return new int[][]{{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
        }
        int[][] arr = new int[adjacency.size()][2];
        for (int i = 0; i < adjacency.size(); i++) {
            arr[i][0] = adjacency.get(i).x();
            arr[i][1] = adjacency.get(i).y();
        }
        return arr;
    }

    private static int[][] copyScreen(int[][] screen, int reelCount, int screenHeight) {
        int[][] copy = new int[reelCount][screenHeight];
        for (int r = 0; r < reelCount; r++) {
            System.arraycopy(screen[r], 0, copy[r], 0, screenHeight);
        }
        return copy;
    }

    private static List<Integer> extract1DimPos(List<List<Integer>> pos2D, int reelCount) {
        TreeSet<Integer> set = new TreeSet<>();
        for (int r = 0; r < pos2D.size(); r++) {
            for (int row : pos2D.get(r)) set.add(reelCount * row + r);
        }
        return new ArrayList<>(set);
    }

    private static List<List<Integer>> extract2DimSym(List<List<Integer>> pos2D, int[][] screen) {
        List<List<Integer>> result = new ArrayList<>(pos2D.size());
        for (int r = 0; r < pos2D.size(); r++) {
            List<Integer> syms = new ArrayList<>();
            for (int row : pos2D.get(r)) syms.add(screen[r][row]);
            result.add(syms);
        }
        return result;
    }

    private static List<Integer> extract1DimSym(List<List<Integer>> contact2DimSym) {
        List<Integer> result = new ArrayList<>();
        for (List<Integer> reel : contact2DimSym) result.addAll(reel);
        return result;
    }

    private static int[] extractStart(List<List<Integer>> pos2D) {
        for (int r = 0; r < pos2D.size(); r++) {
            if (!pos2D.get(r).isEmpty()) return new int[]{r, pos2D.get(r).getFirst()};
        }
        throw new IllegalStateException("No cluster positions found");
    }

    private static TreeMap<Integer, SymbolConfig> normals(SymbolTable symbols) {
        TreeMap<Integer, SymbolConfig> map = new TreeMap<>();
        for (SymbolConfig sc : symbols.all()) {
            if (sc.type() == SymbolType.NORMAL) map.put(sc.symbolId(), sc);
        }
        return map;
    }
}
