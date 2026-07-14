package com.slotslab.simulation.eval;

import com.slotslab.dto.scatters.ContactDto;
import com.slotslab.dto.scatters.ScattersDto;
import com.slotslab.simulation.config.ScattersPaytableEntry;
import com.slotslab.simulation.config.ScattersIntervalSet;
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
 * Symbols pay anywhere on the screen. Win is determined by the total count
 * of matching symbol tiles (including wilds) across all reels and rows.
 * The interval paytable maps count ranges to interval indices; each NORMAL
 * symbol's own paytable (indexed by interval position) provides the multiplier.
 */
public final class ScattersEvaluator {

    private ScattersEvaluator() {}

    public static ScattersDto evalScatters(int[][] screen, int reelCount, int screenHeight,
                                           SymbolTable symbols, int minMatch,
                                           List<ScattersIntervalSet> intervalSets) {
        List<ContactDto> contacts = new ArrayList<>();
        int floatId = 0;

        TreeMap<Integer, SymbolConfig> normals = normals(symbols);
        boolean anyNormalWon = false;

        // Wild-only positions collected for fallback
        List<List<Integer>> wildOnly2DimPos = new ArrayList<>();
        double wildOnlyMultiplierAcc = 1.0;
        int wildOnlyCount = 0;
        for (int r = 0; r < reelCount; r++) {
            List<Integer> posPerReel = new ArrayList<>();
            for (int row = 0; row < screenHeight; row++) {
                int tile = screen[r][row];
                if (symbols.isWild(tile)) {
                    posPerReel.add(row);
                    wildOnlyMultiplierAcc = accumulateWild(symbols.get(tile), wildOnlyMultiplierAcc, wildOnlyCount);
                    wildOnlyCount++;
                }
            }
            wildOnly2DimPos.add(posPerReel);
        }
        int wildOnlyContactCount = wildOnly2DimPos.stream().mapToInt(List::size).sum();

        for (var entry : normals.entrySet()) {
            int sym = entry.getKey();
            SymbolConfig symCfg = entry.getValue();
            List<ScattersPaytableEntry> paytable = resolveSet(intervalSets, symCfg.contactsIntervalSetName());

            List<List<Integer>> contact2DimPos = new ArrayList<>();
            boolean containsNormal = false;
            double wildMultiplierAcc = 1.0;
            int wildCount = 0;

            for (int r = 0; r < reelCount; r++) {
                List<Integer> posPerReel = new ArrayList<>();
                for (int row = 0; row < screenHeight; row++) {
                    int tile = screen[r][row];
                    if (symbols.isScatter(tile)) continue;
                    if (tile == sym) {
                        posPerReel.add(row);
                        containsNormal = true;
                    } else if (symbols.isWild(tile)) {
                        posPerReel.add(row);
                        wildMultiplierAcc = accumulateWild(symbols.get(tile), wildMultiplierAcc, wildCount);
                        wildCount++;
                    }
                }
                contact2DimPos.add(posPerReel);
            }

            int contactSize = contact2DimPos.stream().mapToInt(List::size).sum();
            if (contactSize < minMatch || !containsNormal) continue;

            int intervalIdx = lookupIntervalIndex(paytable, contactSize);
            if (intervalIdx < 0) continue;

            List<Double> symPaytable = symCfg.paytable();
            if (symPaytable == null || intervalIdx >= symPaytable.size()) continue;
            double singularPay = symPaytable.get(intervalIdx);
            if (singularPay <= 0) continue;

            double localMultiplier = wildCount > 0 ? wildMultiplierAcc : 1.0;
            double win = Math.round(singularPay * localMultiplier * 100.0) / 100.0;
            if (win <= 0) continue;

            anyNormalWon = true;

            List<Integer> contact1DimPos = extract1DimPos(contact2DimPos, reelCount);
            List<List<Integer>> contact2DimSym = extract2DimSym(contact2DimPos, screen);
            List<Integer> contact1DimSym = extract1DimSym(contact2DimSym);
            int[] start = extractStart(contact2DimPos);

            contacts.add(new ContactDto(
                    floatId++, contactSize, sym, start[0], start[1],
                    localMultiplier,
                    Math.round(singularPay * 100.0) / 100.0,
                    win,
                    contact1DimPos, contact1DimSym,
                    contact2DimPos, contact2DimSym
            ));
        }

        // Wild-only fallback: wilds alone (no normal connected) pay as the highest-paying normal
        if (!anyNormalWon && wildOnlyCount > 0 && wildOnlyContactCount >= minMatch) {
            SymbolConfig best = highestPayingNormal(normals, intervalSets, wildOnlyContactCount);
            if (best != null) {
                List<ScattersPaytableEntry> bestPaytable = resolveSet(intervalSets, best.contactsIntervalSetName());
                int intervalIdx = lookupIntervalIndex(bestPaytable, wildOnlyContactCount);
                if (intervalIdx >= 0 && best.paytable() != null && intervalIdx < best.paytable().size()) {
                    double singularPay = best.paytable().get(intervalIdx);
                    if (singularPay > 0) {
                        double win = Math.round(singularPay * wildOnlyMultiplierAcc * 100.0) / 100.0;
                        if (win > 0) {
                            List<Integer> contact1DimPos = extract1DimPos(wildOnly2DimPos, reelCount);
                            List<List<Integer>> contact2DimSym = extract2DimSym(wildOnly2DimPos, screen);
                            List<Integer> contact1DimSym = extract1DimSym(contact2DimSym);
                            int[] start = extractStart(wildOnly2DimPos);
                            contacts.add(new ContactDto(
                                    floatId++, wildOnlyContactCount, best.symbolId(), start[0], start[1],
                                    wildOnlyMultiplierAcc,
                                    Math.round(singularPay * 100.0) / 100.0,
                                    win,
                                    contact1DimPos, contact1DimSym,
                                    wildOnly2DimPos, contact2DimSym
                            ));
                        }
                    }
                }
            }
        }

        if (contacts.isEmpty()) return null;
        return ScattersDto.of(contacts, 1.0);
    }

    public static double evalScattersTracked(int[][] screen, int reelCount, int screenHeight,
                                             SymbolTable symbols, int minMatch,
                                             List<ScattersIntervalSet> intervalSets,
                                             Map<ComboKey, long[]> hitMap,
                                             Map<ComboKey, double[]> payMap) {
        double total = 0.0;

        TreeMap<Integer, SymbolConfig> normals = normals(symbols);
        boolean anyNormalWon = false;

        // Count wilds for fallback
        int wildOnlyCount = 0;
        double wildOnlyMultiplierAcc = 1.0;
        for (int r = 0; r < reelCount; r++) {
            for (int row = 0; row < screenHeight; row++) {
                int tile = screen[r][row];
                if (symbols.isWild(tile)) {
                    wildOnlyMultiplierAcc = accumulateWild(symbols.get(tile), wildOnlyMultiplierAcc, wildOnlyCount);
                    wildOnlyCount++;
                }
            }
        }

        for (var entry : normals.entrySet()) {
            int sym = entry.getKey();
            SymbolConfig symCfg = entry.getValue();
            List<ScattersPaytableEntry> paytable = resolveSet(intervalSets, symCfg.contactsIntervalSetName());

            int contactSize = 0;
            boolean containsNormal = false;
            double wildMultiplierAcc = 1.0;
            int wildCount = 0;

            for (int r = 0; r < reelCount; r++) {
                for (int row = 0; row < screenHeight; row++) {
                    int tile = screen[r][row];
                    if (symbols.isScatter(tile)) continue;
                    if (tile == sym) {
                        contactSize++;
                        containsNormal = true;
                    } else if (symbols.isWild(tile)) {
                        contactSize++;
                        wildMultiplierAcc = accumulateWild(symbols.get(tile), wildMultiplierAcc, wildCount);
                        wildCount++;
                    }
                }
            }

            if (contactSize < minMatch || !containsNormal) continue;

            int intervalIdx = lookupIntervalIndex(paytable, contactSize);
            if (intervalIdx < 0) continue;

            List<Double> symPaytable = symCfg.paytable();
            if (symPaytable == null || intervalIdx >= symPaytable.size()) continue;
            double singularPay = symPaytable.get(intervalIdx);
            if (singularPay <= 0) continue;

            double localMultiplier = wildCount > 0 ? wildMultiplierAcc : 1.0;
            double win = Math.round(singularPay * localMultiplier * 100.0) / 100.0;
            if (win <= 0) continue;

            anyNormalWon = true;
            total += win;
            ComboKey key = new ComboKey(sym, contactSize);
            hitMap.computeIfAbsent(key, k -> new long[1])[0]++;
            payMap.computeIfAbsent(key, k -> new double[1])[0] += win;
        }

        // Wild-only fallback
        if (!anyNormalWon && wildOnlyCount > 0 && wildOnlyCount >= minMatch) {
            SymbolConfig best = highestPayingNormal(normals, intervalSets, wildOnlyCount);
            if (best != null) {
                List<ScattersPaytableEntry> bestPaytable = resolveSet(intervalSets, best.contactsIntervalSetName());
                int intervalIdx = lookupIntervalIndex(bestPaytable, wildOnlyCount);
                if (intervalIdx >= 0 && best.paytable() != null && intervalIdx < best.paytable().size()) {
                    double singularPay = best.paytable().get(intervalIdx);
                    if (singularPay > 0) {
                        double win = Math.round(singularPay * wildOnlyMultiplierAcc * 100.0) / 100.0;
                        if (win > 0) {
                            total += win;
                            ComboKey key = new ComboKey(best.symbolId(), wildOnlyCount);
                            hitMap.computeIfAbsent(key, k -> new long[1])[0]++;
                            payMap.computeIfAbsent(key, k -> new double[1])[0] += win;
                        }
                    }
                }
            }
        }

        return total;
    }

    // ── interval paytable lookup ──────────────────────────────────────────────

    /** Resolves the interval list for a symbol: by name if given, otherwise first set. */
    static List<ScattersPaytableEntry> resolveSet(List<ScattersIntervalSet> sets, String name) {
        if (sets == null || sets.isEmpty()) return List.of();
        if (name != null) {
            for (ScattersIntervalSet s : sets) {
                if (name.equals(s.name())) return s.intervals() != null ? s.intervals() : List.of();
            }
        }
        List<ScattersPaytableEntry> first = sets.get(0).intervals();
        return first != null ? first : List.of();
    }

    /** Returns the 0-based interval index for {@code count}, or -1 if no interval covers it. */
    public static int lookupIntervalIndex(List<ScattersPaytableEntry> paytable, int count) {
        if (paytable == null) return -1;
        for (int i = 0; i < paytable.size(); i++) {
            ScattersPaytableEntry e = paytable.get(i);
            if (count >= e.from() && count <= e.to()) return i;
        }
        return -1;
    }

    /** Returns the normal symbol with the highest paytable value at the interval covering {@code count}. */
    private static SymbolConfig highestPayingNormal(TreeMap<Integer, SymbolConfig> normals,
                                                     List<ScattersIntervalSet> intervalSets, int count) {
        SymbolConfig best = null;
        double bestPay = -1;
        for (SymbolConfig cfg : normals.values()) {
            List<ScattersPaytableEntry> pt = resolveSet(intervalSets, cfg.contactsIntervalSetName());
            int idx = lookupIntervalIndex(pt, count);
            if (idx < 0 || cfg.paytable() == null || idx >= cfg.paytable().size()) continue;
            double pay = cfg.paytable().get(idx);
            if (pay > bestPay) { bestPay = pay; best = cfg; }
        }
        return best;
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

    // ── position / symbol extraction helpers ─────────────────────────────────

    private static List<Integer> extract1DimPos(List<List<Integer>> contact2DimPos, int reelCount) {
        TreeSet<Integer> set = new TreeSet<>();
        for (int r = 0; r < contact2DimPos.size(); r++) {
            for (int row : contact2DimPos.get(r)) set.add(reelCount * row + r);
        }
        return new ArrayList<>(set);
    }

    private static List<List<Integer>> extract2DimSym(List<List<Integer>> contact2DimPos, int[][] screen) {
        List<List<Integer>> result = new ArrayList<>(contact2DimPos.size());
        for (int r = 0; r < contact2DimPos.size(); r++) {
            List<Integer> syms = new ArrayList<>();
            for (int row : contact2DimPos.get(r)) syms.add(screen[r][row]);
            result.add(syms);
        }
        return result;
    }

    private static List<Integer> extract1DimSym(List<List<Integer>> contact2DimSym) {
        List<Integer> result = new ArrayList<>();
        for (List<Integer> reel : contact2DimSym) result.addAll(reel);
        return result;
    }

    private static int[] extractStart(List<List<Integer>> contact2DimPos) {
        for (int r = 0; r < contact2DimPos.size(); r++) {
            if (!contact2DimPos.get(r).isEmpty()) {
                return new int[]{r, contact2DimPos.get(r).getFirst()};
            }
        }
        throw new IllegalStateException("No contact positions found");
    }

    private static TreeMap<Integer, SymbolConfig> normals(SymbolTable symbols) {
        TreeMap<Integer, SymbolConfig> map = new TreeMap<>();
        for (SymbolConfig sc : symbols.all()) {
            if (sc.type() == SymbolType.NORMAL) map.put(sc.symbolId(), sc);
        }
        return map;
    }
}
