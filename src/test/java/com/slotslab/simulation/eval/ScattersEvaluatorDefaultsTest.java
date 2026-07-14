package com.slotslab.simulation.eval;

import com.slotslab.dto.scatters.ContactDto;
import com.slotslab.dto.scatters.ContactsDto;
import com.slotslab.simulation.config.ScattersIntervalSet;
import com.slotslab.simulation.config.ScattersPaytableEntry;
import com.slotslab.simulation.config.SymbolConfig;
import com.slotslab.simulation.config.SymbolTable;
import com.slotslab.simulation.config.SymbolType;
import com.slotslab.simulation.config.WildMultiplierAggregation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SCATTERS strategy — 5×5 screen, minMatch=5.
 *
 * Intervals match the UI default placeholders (8–10, 11–14, 15–19, 20–25).
 * Paytable values match the UI geometric placeholder: 0.1, 0.2, 0.4, 0.8.
 *
 *   sym 1  SCATTER  (blocker, no payout)
 *   sym 2  WILD     NONE aggregation
 *   sym 3  NORMAL   paytable [0.1, 0.2, 0.4, 0.8]
 *   sym 4  NORMAL   paytable [0.1, 0.2, 0.4, 0.8]
 */
class ScattersEvaluatorDefaultsTest {

    private static final int REEL_COUNT    = 5;
    private static final int SCREEN_HEIGHT = 5;
    private static final int MIN_MATCH     = 5;

    /** Default UI intervals: 8–10, 11–14, 15–19, 20–25 */
    private static final List<ScattersIntervalSet> INTERVALS = List.of(
            new ScattersIntervalSet("int-set-1", List.of(
                    new ScattersPaytableEntry(8,  10),
                    new ScattersPaytableEntry(11, 14),
                    new ScattersPaytableEntry(15, 19),
                    new ScattersPaytableEntry(20, 25)
            ))
    );

    /** Geometric placeholder paytable: 0.1 × 2^i for 4 intervals */
    private static final List<Double> PAYTABLE = List.of(0.1, 0.2, 0.4, 0.8);

    private static SymbolTable symbols() {
        return new SymbolTable(List.of(
                SymbolConfig.scatter(1),
                new SymbolConfig(2, SymbolType.WILD, List.of(), 1.0, WildMultiplierAggregation.NONE, List.of(), null),
                new SymbolConfig(3, SymbolType.NORMAL, PAYTABLE, 1.0, WildMultiplierAggregation.NONE, List.of(), null),
                new SymbolConfig(4, SymbolType.NORMAL, PAYTABLE, 1.0, WildMultiplierAggregation.NONE, List.of(), null)
        ));
    }

    // ── predefined screen: two normal-symbol wins ─────────────────────────────

    /**
     * Screen (5 reels × 5 rows):
     *   Each reel: [3, 3, 4, 4, 4]
     *   sym3 total = 2×5 = 10  → interval 8–10 (idx 0) → pay = 0.1
     *   sym4 total = 3×5 = 15  → interval 15–19 (idx 2) → pay = 0.4
     */
    @Test
    void twoNormals_bothWin() {
        int[][] screen = {
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4}
        };
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        assertEquals(2, result.contacts().size());

        ContactDto sym3win = result.contacts().stream()
                .filter(c -> c.payoutSymbolId() == 3).findFirst().orElseThrow();
        assertEquals(10, sym3win.contactSize());
        assertEquals(0.1, sym3win.winAmount(), 0.001);
        assertEquals(1.0, sym3win.localMultiplier(), 0.001);

        ContactDto sym4win = result.contacts().stream()
                .filter(c -> c.payoutSymbolId() == 4).findFirst().orElseThrow();
        assertEquals(15, sym4win.contactSize());
        assertEquals(0.4, sym4win.winAmount(), 0.001);
        assertEquals(1.0, sym4win.localMultiplier(), 0.001);
    }

    @Test
    void twoNormals_totalWin_isSumOfBothContacts() {
        int[][] screen = {
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4}
        };
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        double total = result.contacts().stream().mapToDouble(ContactDto::winAmount).sum();
        assertEquals(0.5, total, 0.001); // 0.1 + 0.4
    }

    // ── user-specified screen: [[1,1,2,2,2] × 5 reels] ───────────────────────

    /**
     * User screen — reinterpreted with sym1=NORMAL, sym2=NORMAL:
     *   Each reel: [1, 1, 2, 2, 2]
     *   sym1 total = 2×5 = 10  → interval 8–10 (idx 0) → pay = 0.1
     *   sym2 total = 3×5 = 15  → interval 15–19 (idx 2) → pay = 0.4
     * Uses a symbol table where sym1 and sym2 are both NORMAL (no scatter/wild).
     */
    @Test
    void userScreen_twoNormals_bothWin() {
        SymbolTable allNormals = new SymbolTable(List.of(
                new SymbolConfig(1, SymbolType.NORMAL, PAYTABLE, 1.0, WildMultiplierAggregation.NONE, List.of(), null),
                new SymbolConfig(2, SymbolType.NORMAL, PAYTABLE, 1.0, WildMultiplierAggregation.NONE, List.of(), null)
        ));
        int[][] screen = {
                {1, 1, 2, 2, 2},
                {1, 1, 2, 2, 2},
                {1, 1, 2, 2, 2},
                {1, 1, 2, 2, 2},
                {1, 1, 2, 2, 2}
        };
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                allNormals, MIN_MATCH, INTERVALS);

        assertNotNull(result);
        assertEquals(2, result.contacts().size());

        ContactDto sym1win = result.contacts().stream()
                .filter(c -> c.payoutSymbolId() == 1).findFirst().orElseThrow();
        assertEquals(10, sym1win.contactSize());
        assertEquals(0.1, sym1win.winAmount(), 0.001);

        ContactDto sym2win = result.contacts().stream()
                .filter(c -> c.payoutSymbolId() == 2).findFirst().orElseThrow();
        assertEquals(15, sym2win.contactSize());
        assertEquals(0.4, sym2win.winAmount(), 0.001);
    }

    /**
     * User screen with default symbol table (sym1=SCATTER, sym2=WILD/NONE):
     *   Each reel: [1, 1, 2, 2, 2]
     *   sym1 → scatter (blocker): 10 blocked tiles
     *   sym2 → wild (NONE agg): 15 wild-only tiles, no normal present
     *   Wild-only fallback fires: count=15 → interval 15–19 (idx 2) → best normal pay = 0.4
     */
    @Test
    void userScreen_defaultSymbols_scatterBlocksWildOnlyFallback() {
        int[][] screen = {
                {1, 1, 2, 2, 2},
                {1, 1, 2, 2, 2},
                {1, 1, 2, 2, 2},
                {1, 1, 2, 2, 2},
                {1, 1, 2, 2, 2}
        };
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        assertEquals(1, result.contacts().size());
        ContactDto c = result.contacts().get(0);
        assertEquals(15, c.contactSize());
        assertEquals(0.4, c.winAmount(), 0.001);
        assertEquals(1.0, c.localMultiplier(), 0.001);
    }

    // ── interval boundary coverage ────────────────────────────────────────────

    /** count=8 → interval 8–10 (idx 0) → pay = 0.1 */
    @Test
    void count8_interval0_pay01() {
        // 8 sym3 tiles: reels 0..3 each have [3,3,0,0,0], reel4 = [0,0,0,0,0] (pad with sym4 = no sym4 win since <5)
        // Simpler: 4 reels with 2 sym3 each = 8; reel4 has sym4 only (4 tiles) → sym4 count=4 < minMatch=5
        SymbolTable sym3only = new SymbolTable(List.of(
                new SymbolConfig(3, SymbolType.NORMAL, PAYTABLE, 1.0, WildMultiplierAggregation.NONE, List.of(), null),
                new SymbolConfig(4, SymbolType.NORMAL, PAYTABLE, 1.0, WildMultiplierAggregation.NONE, List.of(), null)
        ));
        // [[3,3,4,4,4],[3,3,4,4,4],[3,3,4,4,4],[3,3,4,4,4],[4,4,4,4,4]]: sym3=8, sym4=17
        int[][] screen = {
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4},
                {3, 3, 4, 4, 4},
                {4, 4, 4, 4, 4}
        };
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                sym3only, MIN_MATCH, INTERVALS);

        assertNotNull(result);
        ContactDto sym3win = result.contacts().stream()
                .filter(c -> c.payoutSymbolId() == 3).findFirst().orElseThrow();
        assertEquals(8, sym3win.contactSize());
        assertEquals(0.1, sym3win.winAmount(), 0.001);
    }

    /** count=20 → interval 20–25 (idx 3) → pay = 0.8 */
    @Test
    void count20_interval3_pay08() {
        SymbolTable sym3only = new SymbolTable(List.of(
                new SymbolConfig(3, SymbolType.NORMAL, PAYTABLE, 1.0, WildMultiplierAggregation.NONE, List.of(), null)
        ));
        // 4 sym3 per reel × 5 reels = 20
        int[][] screen = {
                {3, 3, 3, 3, 3},
                {3, 3, 3, 3, 3},
                {3, 3, 3, 3, 3},
                {3, 3, 3, 3, 3},
                {3, 3, 3, 3, 3}
        };
        // total = 25, interval 20-25 (idx 3)
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                sym3only, MIN_MATCH, INTERVALS);

        assertNotNull(result);
        ContactDto c = result.contacts().get(0);
        assertEquals(25, c.contactSize());
        assertEquals(0.8, c.winAmount(), 0.001);
    }

    /** count=7 < minMatch=5 but also below interval floor (8) → no win */
    @Test
    void countBelowIntervalFloor_noWin() {
        SymbolTable sym3only = new SymbolTable(List.of(
                new SymbolConfig(3, SymbolType.NORMAL, PAYTABLE, 1.0, WildMultiplierAggregation.NONE, List.of(), null),
                SymbolConfig.scatter(99) // filler to avoid empty symbol table edge
        ));
        // 7 sym3 tiles: reels 0..1 full sym3 (5+5=10)... need exactly 7
        // reel0=[3,3,3,3,3]=5, reel1=[3,3,99,99,99]=2 → sym3=7; rest scatter
        int[][] screen = {
                {3, 3, 3, 3, 3},
                {3, 3, 99, 99, 99},
                {99, 99, 99, 99, 99},
                {99, 99, 99, 99, 99},
                {99, 99, 99, 99, 99}
        };
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                sym3only, MIN_MATCH, INTERVALS);
        assertNull(result);
    }

    // ── wild (NONE agg) connects with normal ──────────────────────────────────

    /**
     * sym2=WILD(NONE) augments sym3 contact count; multiplier stays 1.0.
     * sym3=5, sym2=10 → contact=15 → interval 15–19 (idx 2) → pay = 0.4
     */
    @Test
    void wildNoneAgg_connectsWithNormal_multiplierStaysOne() {
        int[][] screen = {
                {3, 3, 3, 3, 3},   // 5 sym3
                {2, 2, 2, 2, 2},   // 5 wilds
                {2, 2, 2, 2, 2},   // 5 wilds
                {4, 4, 4, 4, 4},   // 5 sym4 (separate win)
                {4, 4, 4, 4, 4}    // 5 sym4
        };
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);

        ContactDto sym3win = result.contacts().stream()
                .filter(c -> c.payoutSymbolId() == 3).findFirst().orElseThrow();
        assertEquals(15, sym3win.contactSize()); // 5 sym3 + 10 wilds
        assertEquals(1.0, sym3win.localMultiplier(), 0.001);
        assertEquals(0.4, sym3win.winAmount(), 0.001);

        // sym4 contact: 10 wilds from reels 1-2 + 10 sym4 from reels 3-4 = 20 → interval 20-25 (idx 3)
        ContactDto sym4win = result.contacts().stream()
                .filter(c -> c.payoutSymbolId() == 4).findFirst().orElseThrow();
        assertEquals(20, sym4win.contactSize());
        assertEquals(0.8, sym4win.winAmount(), 0.001);
    }

    // ── scatter tiles block both normals ──────────────────────────────────────

    @Test
    void scatterTilesReduceContactCount() {
        // Without scatter: sym3=25 → pay 0.8. With 5 scatters sym3 drops to 20 → still pay 0.8
        int[][] screen = {
                {1, 3, 3, 3, 3},  // 1 scatter + 4 sym3
                {3, 3, 3, 3, 3},
                {3, 3, 3, 3, 3},
                {3, 3, 3, 3, 3},
                {3, 3, 3, 3, 3}
        };
        ContactsDto result = ScattersEvaluator.evalScatters(screen, REEL_COUNT, SCREEN_HEIGHT,
                symbols(), MIN_MATCH, INTERVALS);

        assertNotNull(result);
        ContactDto c = result.contacts().stream()
                .filter(x -> x.payoutSymbolId() == 3).findFirst().orElseThrow();
        assertEquals(24, c.contactSize()); // 25 - 1 scatter
        assertEquals(0.8, c.winAmount(), 0.001); // still interval 20-25
    }
}
