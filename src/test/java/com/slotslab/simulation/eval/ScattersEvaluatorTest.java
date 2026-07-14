package com.slotslab.simulation.eval;

import com.slotslab.dto.scatters.ContactDto;
import com.slotslab.dto.scatters.ContactsDto;
import com.slotslab.simulation.config.ScattersIntervalSet;
import com.slotslab.simulation.config.ScattersPaytableEntry;
import com.slotslab.simulation.config.SymbolConfig;
import com.slotslab.simulation.config.SymbolTable;
import com.slotslab.simulation.config.SymbolType;
import com.slotslab.simulation.config.WildMultiplierAggregation;
import com.slotslab.simulation.stats.ComboKey;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Grid is 3×3 (reelCount=3, screenHeight=3), minMatch=3.
 *
 *   sym 1  NORMAL  paytable [5.0, 10.0]  intervals: 3–5, 6–9
 *   sym 2  NORMAL  paytable [2.0,  4.0]  same intervals
 *   sym 3  SCATTER (blocker, no payout)
 *   sym 4  WILD    aggregation NONE
 *   sym 5  WILD    aggregation MULTIPLY  multiplier=2.0
 *   sym 6  WILD    aggregation SEQUENCE  sequence=[2.0, 3.0]
 */
class ScattersEvaluatorTest {

    private static final int REEL_COUNT    = 3;
    private static final int SCREEN_HEIGHT = 3;
    private static final int MIN_MATCH     = 3;

    private static final List<ScattersIntervalSet> INTERVALS = List.of(
            new ScattersIntervalSet("default", List.of(
                    new ScattersPaytableEntry(3, 5),
                    new ScattersPaytableEntry(6, 9)
            ))
    );

    private static SymbolTable symbols() {
        return new SymbolTable(List.of(
                new SymbolConfig(1, SymbolType.NORMAL, List.of(5.0, 10.0), 1.0, WildMultiplierAggregation.ADD, List.of(), null),
                new SymbolConfig(2, SymbolType.NORMAL, List.of(2.0,  4.0), 1.0, WildMultiplierAggregation.ADD, List.of(), null),
                SymbolConfig.scatter(3),
                new SymbolConfig(4, SymbolType.WILD, List.of(), 1.0, WildMultiplierAggregation.NONE, List.of(), null)
        ));
    }

    // ── basic wins ────────────────────────────────────────────────────────────

    @Test
    void allSym1_minMatch_wins() {
        // 3×3 full of sym1 → contactSize=9, interval[1] (6–9), pay=10.0
        int[][] screen = {{1,1,1},{1,1,1},{1,1,1}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        assertEquals(1, result.contacts().size());
        ContactDto c = result.contacts().get(0);
        assertEquals(1, c.payoutSymbolId());
        assertEquals(9, c.contactSize());
        assertEquals(10.0, c.winAmount(), 0.001);
    }

    @Test
    void exactlyMinMatch_sym2_wins() {
        // sym2 on only 3 tiles, rest sym1
        // screen: [[2,1,1],[1,1,1],[1,1,1]] → sym2 count=1 < 3 → no sym2 win
        // Actually need sym2 ≥ 3. Use: [[2,2,2],[1,1,1],[1,1,1]] sym2=3, sym1 count includes 6 tiles
        int[][] screen = {{2,2,2},{1,1,1},{1,1,1}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        // sym1: count=6, interval[1], pay=10.0; sym2: count=3, interval[0], pay=2.0
        ContactDto sym1 = result.contacts().stream().filter(c -> c.payoutSymbolId() == 1).findFirst().orElseThrow();
        ContactDto sym2 = result.contacts().stream().filter(c -> c.payoutSymbolId() == 2).findFirst().orElseThrow();
        assertEquals(6, sym1.contactSize());
        assertEquals(10.0, sym1.winAmount(), 0.001);
        assertEquals(3, sym2.contactSize());
        assertEquals(2.0, sym2.winAmount(), 0.001);
    }

    @Test
    void noNormalMeetsMinMatch_returnsNull() {
        // only 2 of sym1 → no win
        int[][] screen = {{1,1,3},{3,3,3},{3,3,3}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);
        assertNull(result);
    }

    // ── scatter tiles are blocked ─────────────────────────────────────────────

    @Test
    void scatterTilesDoNotCount() {
        // sym1=5 tiles, but one is replaced by scatter → sym1 effective=4, still wins
        // [[1,1,1],[1,3,1],[1,1,1]] → sym1 count=8 (scatter at [1][1] skipped)
        int[][] screen = {{1,1,1},{1,3,1},{1,1,1}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        ContactDto sym1 = result.contacts().stream().filter(c -> c.payoutSymbolId() == 1).findFirst().orElseThrow();
        assertEquals(8, sym1.contactSize());
    }

    @Test
    void allScatter_noWin() {
        int[][] screen = {{3,3,3},{3,3,3},{3,3,3}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);
        assertNull(result);
    }

    // ── wild connects with normals ────────────────────────────────────────────

    @Test
    void wildConnectsToNormal_noneAggregation() {
        // sym4=WILD(NONE). [[4,4,4],[1,1,1],[3,3,3]] → sym1 contact = 3 wilds + 3 sym1 = 6
        // multiplier stays 1.0 (NONE), pay=interval[1]=10.0
        int[][] screen = {{4,4,4},{1,1,1},{3,3,3}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        ContactDto sym1 = result.contacts().stream().filter(c -> c.payoutSymbolId() == 1).findFirst().orElseThrow();
        assertEquals(6, sym1.contactSize());
        assertEquals(1.0, sym1.localMultiplier(), 0.001);
        assertEquals(10.0, sym1.winAmount(), 0.001);
    }

    @Test
    void wildMultiply_doublesWin() {
        SymbolTable symbols = new SymbolTable(List.of(
                new SymbolConfig(1, SymbolType.NORMAL, List.of(5.0, 10.0), 1.0, WildMultiplierAggregation.ADD, List.of(), null),
                SymbolConfig.scatter(3),
                new SymbolConfig(5, SymbolType.WILD, List.of(), 2.0, WildMultiplierAggregation.MULTIPLY, List.of(), null)
        ));
        // [[5,1,1],[1,1,1],[1,1,1]] → sym1 contact = 1 wild + 8 sym1 = 9, mult=2.0
        int[][] screen = {{5,1,1},{1,1,1},{1,1,1}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols, MIN_MATCH, INTERVALS);

        assertNotNull(result);
        ContactDto sym1 = result.contacts().get(0);
        assertEquals(9, sym1.contactSize());
        assertEquals(2.0, sym1.localMultiplier(), 0.001);
        assertEquals(20.0, sym1.winAmount(), 0.001); // 10.0 * 2.0
    }

    @Test
    void wildSequence_twoWilds_usesSecondSequenceEntry() {
        SymbolTable symbols = new SymbolTable(List.of(
                new SymbolConfig(1, SymbolType.NORMAL, List.of(5.0, 10.0), 1.0, WildMultiplierAggregation.ADD, List.of(), null),
                SymbolConfig.scatter(3),
                new SymbolConfig(6, SymbolType.WILD, List.of(), 0.0, WildMultiplierAggregation.SEQUENCE,
                        List.of(2.0, 3.0), null)
        ));
        // [[6,6,1],[1,1,1],[1,1,1]] → sym1 contact = 2 wilds + 7 sym1 = 9, mult=seq[1]=3.0
        int[][] screen = {{6,6,1},{1,1,1},{1,1,1}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols, MIN_MATCH, INTERVALS);

        assertNotNull(result);
        ContactDto sym1 = result.contacts().get(0);
        assertEquals(3.0, sym1.localMultiplier(), 0.001);
        assertEquals(30.0, sym1.winAmount(), 0.001); // 10.0 * 3.0
    }

    // ── wild-only fallback ────────────────────────────────────────────────────

    @Test
    void wildOnlyFallback_paysAsHighestNormal() {
        // All wilds, no normal → fallback to highest paying normal for count
        // [[4,4,4],[4,4,4],[4,4,4]] → wildOnlyCount=9, best normal=sym1, interval[1]=10.0
        int[][] screen = {{4,4,4},{4,4,4},{4,4,4}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        assertEquals(1, result.contacts().size());
        ContactDto c = result.contacts().get(0);
        assertEquals(1, c.payoutSymbolId()); // pays as highest normal (sym1)
        assertEquals(10.0, c.winAmount(), 0.001);
    }

    @Test
    void wildOnlyFallback_notFiredWhenNormalWins() {
        // sym1 wins → wild-only fallback must NOT fire (no duplicate entry)
        // [[4,1,1],[1,1,1],[1,1,1]] → sym1 wins; wilds connect
        int[][] screen = {{4,1,1},{1,1,1},{1,1,1}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        // no wild-only fallback entry — only sym1 win with wild absorbed
        long wildOnlyEntries = result.contacts().stream()
                .filter(c -> c.contactSize() == 1) // 1 wild wouldn't make a stand-alone entry
                .count();
        // The real check: no extra "wild-only" entry appears alongside sym1
        assertTrue(result.contacts().stream().anyMatch(c -> c.payoutSymbolId() == 1));
        // sym1 win absorbs the wild — no separate wild-only contact
        assertEquals(1, result.contacts().size());
    }

    @Test
    void wildOnlyFallback_belowMinMatch_noWin() {
        // Only 2 wilds, rest scatter → count=2 < minMatch=3
        int[][] screen = {{4,4,3},{3,3,3},{3,3,3}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);
        assertNull(result);
    }

    // ── interval lookup ───────────────────────────────────────────────────────

    @Test
    void lookupIntervalIndex_exactBounds() {
        List<ScattersPaytableEntry> pt = INTERVALS.get(0).intervals();
        assertEquals(0, ScattersEvaluator.lookupIntervalIndex(pt, 3));
        assertEquals(0, ScattersEvaluator.lookupIntervalIndex(pt, 5));
        assertEquals(1, ScattersEvaluator.lookupIntervalIndex(pt, 6));
        assertEquals(1, ScattersEvaluator.lookupIntervalIndex(pt, 9));
    }

    @Test
    void lookupIntervalIndex_outsideAllIntervals_returnsMinusOne() {
        List<ScattersPaytableEntry> pt = INTERVALS.get(0).intervals();
        assertEquals(-1, ScattersEvaluator.lookupIntervalIndex(pt, 2));
        assertEquals(-1, ScattersEvaluator.lookupIntervalIndex(pt, 10));
    }

    @Test
    void lookupIntervalIndex_nullList_returnsMinusOne() {
        assertEquals(-1, ScattersEvaluator.lookupIntervalIndex(null, 5));
    }

    // ── tracked variant ───────────────────────────────────────────────────────

    @Test
    void evalScattersTracked_recordsHitsAndPay() {
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();
        int[][] screen = {{1,1,1},{1,1,1},{1,1,1}};
        double win = ScattersEvaluator.evalScattersTracked(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS, hitMap, payMap);

        assertEquals(10.0, win, 0.001);
        long hits = hitMap.getOrDefault(new ComboKey(1, 9), new long[]{0})[0];
        assertEquals(1, hits);
        double pay = payMap.getOrDefault(new ComboKey(1, 9), new double[]{0.0})[0];
        assertEquals(10.0, pay, 0.001);
    }

    @Test
    void evalScattersTracked_wildOnlyFallback_tracksAsBestNormal() {
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();
        int[][] screen = {{4,4,4},{4,4,4},{4,4,4}};
        double win = ScattersEvaluator.evalScattersTracked(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS, hitMap, payMap);

        assertEquals(10.0, win, 0.001);
        // tracked under sym1 (highest paying normal) with count=9
        long hits = hitMap.getOrDefault(new ComboKey(1, 9), new long[]{0})[0];
        assertEquals(1, hits);
    }

    // ── per-symbol interval sets ──────────────────────────────────────────────

    @Test
    void perSymbolIntervalSet_usesOwnSet() {
        // sym1 uses "high" set, sym2 uses "low" set
        List<ScattersIntervalSet> sets = List.of(
                new ScattersIntervalSet("high", List.of(new ScattersPaytableEntry(3, 9))),
                new ScattersIntervalSet("low",  List.of(new ScattersPaytableEntry(3, 9)))
        );
        SymbolTable symbols = new SymbolTable(List.of(
                new SymbolConfig(1, SymbolType.NORMAL, List.of(8.0), 1.0, WildMultiplierAggregation.ADD, List.of(), "high"),
                new SymbolConfig(2, SymbolType.NORMAL, List.of(1.0), 1.0, WildMultiplierAggregation.ADD, List.of(), "low"),
                SymbolConfig.scatter(3)
        ));
        // [[1,1,1],[2,2,2],[3,3,3]] → sym1 count=3 → interval "high"[0] → pay=8.0
        //                           → sym2 count=3 → interval "low"[0]  → pay=1.0
        int[][] screen = {{1,1,1},{2,2,2},{3,3,3}};
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols, MIN_MATCH, sets);

        assertNotNull(result);
        ContactDto sym1c = result.contacts().stream().filter(c -> c.payoutSymbolId() == 1).findFirst().orElseThrow();
        ContactDto sym2c = result.contacts().stream().filter(c -> c.payoutSymbolId() == 2).findFirst().orElseThrow();
        assertEquals(8.0, sym1c.winAmount(), 0.001);
        assertEquals(1.0, sym2c.winAmount(), 0.001);
    }
}
