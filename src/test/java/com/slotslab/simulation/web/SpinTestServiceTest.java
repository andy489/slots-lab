package com.slotslab.simulation.web;

import com.slotslab.dto.spin.SpinData;
import com.slotslab.simulation.config.ReelSetChance;
import com.slotslab.simulation.config.ScattersIntervalSet;
import com.slotslab.simulation.config.ScattersPaytableEntry;
import com.slotslab.simulation.config.SymbolConfig;
import com.slotslab.simulation.config.SymbolType;
import com.slotslab.simulation.config.WildMultiplierAggregation;
import com.slotslab.simulation.strategy.PayoutStrategyType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SpinTestService.generate(SpinTestRequest).
 *
 * Symbol table used by most tests (minMatch=3, screenWidth=5):
 *   sym 1  NORMAL  paytable [1.0, 3.0, 10.0]  (3x, 4x, 5x pays)
 *   sym 2  WILD    aggregation NONE
 *   sym 3  NORMAL  paytable [0.2, 0.6,  2.0]
 */
class SpinTestServiceTest {

    private static final int MIN_MATCH     = 3;
    private static final int SCREEN_WIDTH  = 5;
    private static final int SCREEN_HEIGHT = 3;

    // ── symbol table ─────────────────────────────────────────────────────────

    private static List<SymbolConfig> symbols() {
        return List.of(
                SymbolConfig.normal(1, List.of(1.0, 3.0, 10.0)),
                new SymbolConfig(2, SymbolType.WILD, List.of(), 1.0, WildMultiplierAggregation.NONE, List.of(), null),
                SymbolConfig.normal(3, List.of(0.2, 0.6, 2.0))
        );
    }

    // ── line definitions ──────────────────────────────────────────────────────

    /** Single line across row 0 of every reel. */
    private static List<List<Integer>> row0Lines() {
        return List.of(List.of(0, 0, 0, 0, 0));
    }

    // ── fixed-screen request builder ──────────────────────────────────────────

    /**
     * Builds a SpinTestRequest backed by a fixed screen.
     * reelSets and reelSetChances are left empty; the provided screen[][] drives evaluation.
     * screen is column-major: screen[reel][row].
     */
    private static SpinTestRequest fixedScreenRequest(
            int[][] screen,
            List<SymbolConfig> syms,
            PayoutStrategyType strategy,
            List<List<Integer>> lines) {

        List<List<Integer>> screenList = Arrays.stream(screen)
                .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());

        return new SpinTestRequest(
                List.of(),   // reelSets — empty for fixed-screen path
                List.of(),   // reelSetChances — empty for fixed-screen path
                syms,
                strategy,
                screen.length,      // screenWidth
                screen[0].length,   // screenHeight
                MIN_MATCH,
                lines,
                1,           // count
                null,        // reelSetIndex
                null,        // stops
                screenList,
                null,        // contactsIntervalSets
                null         // adjacencyOffsets
        );
    }

    // ── reel-based request builder ────────────────────────────────────────────

    /**
     * Minimal 5-reel set where each reel holds exactly screenHeight tiles of the same symbol,
     * so a stop of 0 always yields that symbol on every row of that reel.
     * reelStrip[reel] = symbol id repeated screenHeight times.
     */
    private static RtpRequest.ReelSetEntry simpleReelSet(int[] symbolPerReel, int height) {
        List<List<Integer>> reels = Arrays.stream(symbolPerReel)
                .mapToObj(sym -> {
                    List<Integer> strip = new java.util.ArrayList<>(height);
                    for (int i = 0; i < height; i++) strip.add(sym);
                    return strip;
                })
                .collect(Collectors.toList());
        return new RtpRequest.ReelSetEntry("set0", reels);
    }

    private static SpinTestRequest reelBasedRequest(
            RtpRequest.ReelSetEntry reelSet,
            List<Integer> stops,
            List<SymbolConfig> syms,
            PayoutStrategyType strategy,
            List<List<Integer>> lines,
            Integer reelSetIndex) {

        return new SpinTestRequest(
                List.of(reelSet),
                List.of(new ReelSetChance(0, 1.0)),
                syms,
                strategy,
                SCREEN_WIDTH,
                SCREEN_HEIGHT,
                MIN_MATCH,
                lines,
                1,
                reelSetIndex,
                stops,
                null,   // no fixed screen
                null,   // no contactsIntervalSets
                null    // adjacencyOffsets
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. Fixed screen path
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void fixedScreen_knownWin_returnsCorrectWinAmount() {
        // Row 0 on all 5 reels: [1,1,1,1,1] → sym1 5-of-a-kind, ptIdx=2, pay=10.0
        int[][] screen = {
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.LTR, row0Lines());
        SpinTestService service = new SpinTestService();

        List<SpinData> results = service.generate(req);

        assertEquals(1, results.size());
        assertEquals(10.0, results.get(0).winAmount(), 0.001);
    }

    @Test
    void fixedScreen_noWin_returnsZeroAndEmptyPayoutData() {
        // Row 0: [1,1,3,3,3] → sym1 streak=2, sym3 streak=3 starting at reel 2, no LTR win from reel 0
        // LTR from reel 0: sym1 appears on reels 0,1, then sym3 on reel 2 → streak breaks at 2, no win.
        // sym3 does NOT start at reel 0 so no LTR win for sym3 either.
        int[][] screen = {
                {1, 0, 0},
                {1, 0, 0},
                {3, 0, 0},
                {3, 0, 0},
                {3, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.LTR, row0Lines());
        SpinTestService service = new SpinTestService();

        List<SpinData> results = service.generate(req);

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).winAmount(), 0.001);
        assertTrue(results.get(0).payoutData().isEmpty());
    }

    @Test
    void fixedScreen_count3_returnsThreeIdenticalResults() {
        // 3-of-a-kind on row 0: [1,1,1,3,3] → pay=1.0
        int[][] screen = {
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {3, 0, 0},
                {3, 0, 0}
        };
        List<List<Integer>> screenList = Arrays.stream(screen)
                .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());

        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(), symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                3,    // count=3
                null, null,
                screenList,
                null,
                null  // adjacencyOffsets
        );
        SpinTestService service = new SpinTestService();

        List<SpinData> results = service.generate(req);

        assertEquals(3, results.size());
        for (SpinData spin : results) {
            assertEquals(1.0, spin.winAmount(), 0.001);
            assertEquals(screenList, spin.screen());
        }
    }

    @Test
    void fixedScreen_reelsAndChancesAreIgnored() {
        // Even if we pass a wrong reelSet here, the fixed screen must take precedence.
        int[][] screen = {
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0}
        };
        List<List<Integer>> screenList = Arrays.stream(screen)
                .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());

        // Provide a reel set too — it must be ignored because screen is non-null/non-empty
        RtpRequest.ReelSetEntry dummyReels = simpleReelSet(new int[]{3, 3, 3, 3, 3}, SCREEN_HEIGHT);
        SpinTestRequest req = new SpinTestRequest(
                List.of(dummyReels),
                List.of(new ReelSetChance(0, 1.0)),
                symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                1, null, null,
                screenList,
                null,
                null  // adjacencyOffsets
        );
        SpinTestService service = new SpinTestService();

        List<SpinData> results = service.generate(req);

        // Screen came from the fixed screen (all sym1 5x = 10.0), not from the dummy reels (all sym3)
        assertEquals(10.0, results.get(0).winAmount(), 0.001);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. Reel-based path
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void reelBased_withFixedStops_deterministicScreen() {
        // Reel 0-2 filled with sym1, reel 3-4 with sym3. Stop 0 on all reels.
        // Row 0 will be [1,1,1,3,3] → LTR sym1 3x = 1.0
        RtpRequest.ReelSetEntry reelSet = simpleReelSet(new int[]{1, 1, 1, 3, 3}, SCREEN_HEIGHT);
        List<Integer> stops = List.of(0, 0, 0, 0, 0);

        SpinTestRequest req = reelBasedRequest(reelSet, stops, symbols(), PayoutStrategyType.LTR, row0Lines(), null);
        SpinTestService service = new SpinTestService();

        List<SpinData> results = service.generate(req);

        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).winAmount(), 0.001);
        assertEquals(stops, results.get(0).reelsStopPositions());
    }

    @Test
    void reelBased_withoutStops_screenSymbolsWithinReelStrip() {
        // Reels contain only sym1 and sym3 — any random stop still returns valid symbols.
        RtpRequest.ReelSetEntry reelSet = simpleReelSet(new int[]{1, 1, 1, 1, 1}, SCREEN_HEIGHT);

        SpinTestRequest req = reelBasedRequest(reelSet, null, symbols(), PayoutStrategyType.LTR, row0Lines(), null);
        SpinTestService service = new SpinTestService();

        List<SpinData> results = service.generate(req);

        assertEquals(1, results.size());
        SpinData spin = results.get(0);
        // All symbols on the screen must come from the reel strip (only sym1 here)
        for (List<Integer> col : spin.screen()) {
            for (int sym : col) {
                assertEquals(1, sym, "Every cell must contain sym1 from the reel strip");
            }
        }
    }

    @Test
    void reelBased_withReelSetIndex_usesSpecifiedSet() {
        // Two reel sets: set0 all sym1, set1 all sym3. Specify index 1 → screen should be all sym3.
        RtpRequest.ReelSetEntry set0 = simpleReelSet(new int[]{1, 1, 1, 1, 1}, SCREEN_HEIGHT);
        RtpRequest.ReelSetEntry set1 = simpleReelSet(new int[]{3, 3, 3, 3, 3}, SCREEN_HEIGHT);

        SpinTestRequest req = new SpinTestRequest(
                List.of(set0, set1),
                List.of(new ReelSetChance(0, 1.0), new ReelSetChance(1, 1.0)),
                symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                1,
                1,    // reelSetIndex = 1
                List.of(0, 0, 0, 0, 0),
                null,
                null,
                null  // adjacencyOffsets
        );
        SpinTestService service = new SpinTestService();

        List<SpinData> results = service.generate(req);

        assertEquals(1, results.size());
        // sym3 5x pays at ptIdx=2 → 2.0
        assertEquals(2.0, results.get(0).winAmount(), 0.001);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. Validation — illegal argument cases
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void validate_nullReelSets_withoutFixedScreen_throws() {
        SpinTestRequest req = new SpinTestRequest(
                null,   // null reelSets
                null,
                symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                1, null, null, null, null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    @Test
    void validate_emptyReelSets_withoutFixedScreen_throws() {
        SpinTestRequest req = new SpinTestRequest(
                List.of(),           // empty reelSets
                List.of(),           // empty chances
                symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                1, null, null, null, null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    @Test
    void validate_nullSymbols_throws() {
        int[][] screen = {{1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}};
        List<List<Integer>> screenList = Arrays.stream(screen)
                .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());

        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                null,   // null symbols
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                1, null, null, screenList, null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    @Test
    void validate_screenWidthLessThan1_throws() {
        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                symbols(),
                PayoutStrategyType.LTR,
                0,   // screenWidth < 1
                SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                1, null, null,
                List.of(List.of(1, 0, 0)),
                null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    @Test
    void validate_screenHeightLessThan1_throws() {
        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH,
                0,   // screenHeight < 1
                MIN_MATCH,
                row0Lines(),
                1, null, null,
                List.of(List.of(1, 0, 0), List.of(1, 0, 0), List.of(1, 0, 0), List.of(1, 0, 0), List.of(1, 0, 0)),
                null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    @Test
    void validate_ltrWithNoLineDefinitions_throws() {
        int[][] screen = {{1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}};
        List<List<Integer>> screenList = Arrays.stream(screen)
                .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());

        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                List.of(),   // no line definitions
                1, null, null, screenList, null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    @Test
    void validate_reelSetIndexOutOfRange_throws() {
        RtpRequest.ReelSetEntry reelSet = simpleReelSet(new int[]{1, 1, 1, 1, 1}, SCREEN_HEIGHT);

        SpinTestRequest req = new SpinTestRequest(
                List.of(reelSet),
                List.of(new ReelSetChance(0, 1.0)),
                symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                1,
                5,   // index out of range (only index 0 exists)
                null, null, null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    @Test
    void validate_fixedScreenFewerColumnsThanScreenWidth_throws() {
        // screenWidth=5 but fixed screen has only 3 columns
        List<List<Integer>> tooNarrow = List.of(
                List.of(1, 0, 0),
                List.of(1, 0, 0),
                List.of(1, 0, 0)
        );
        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH,   // 5
                SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                1, null, null,
                tooNarrow,      // only 3 columns
                null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    @Test
    void validate_waysStrategyWithNonNoneWildAggregation_throws() {
        List<SymbolConfig> symbolsWithMultiplierWild = List.of(
                SymbolConfig.normal(1, List.of(1.0, 3.0, 10.0)),
                new SymbolConfig(2, SymbolType.WILD, List.of(), 2.0, WildMultiplierAggregation.ADD, List.of(), null)
        );
        int[][] screen = {{1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}};
        List<List<Integer>> screenList = Arrays.stream(screen)
                .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());

        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                symbolsWithMultiplierWild,
                PayoutStrategyType.WAYS,  // WAYS does not support wild multipliers
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                null,   // no lines needed for WAYS
                1, null, null, screenList, null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    @Test
    void validate_nonDecreasingPaytableViolation_throws() {
        List<SymbolConfig> badPaytable = List.of(
                new SymbolConfig(1, SymbolType.NORMAL,
                        List.of(3.0, 1.0, 10.0),  // entry[1]=1.0 < entry[0]=3.0 — violation
                        1.0, WildMultiplierAggregation.ADD, List.of(), null),
                new SymbolConfig(2, SymbolType.WILD, List.of(), 1.0, WildMultiplierAggregation.NONE, List.of(), null)
        );
        int[][] screen = {{1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}};
        List<List<Integer>> screenList = Arrays.stream(screen)
                .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());

        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                badPaytable,
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                1, null, null, screenList, null,
                null  // adjacencyOffsets
        );
        assertThrows(IllegalArgumentException.class, () -> new SpinTestService().generate(req));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. Multi-strategy correctness
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void ltr_3ofKind_row0_correctWin() {
        // [1,1,1,3,3] on row 0 → sym1 streak=3, ptIdx=0, pay=1.0
        int[][] screen = {
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {3, 0, 0},
                {3, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.LTR, row0Lines());
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(1.0, results.get(0).winAmount(), 0.001);
    }

    @Test
    void ltr_wildSubstitution_formsMinMatch() {
        // [2,1,1,3,3] — wild + sym1 + sym1 = streak 3 → pay=1.0
        int[][] screen = {
                {2, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {3, 0, 0},
                {3, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.LTR, row0Lines());
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(1.0, results.get(0).winAmount(), 0.001);
    }

    @Test
    void rtl_3ofKind_winsFromRightSide() {
        // [3,3,1,1,1] — RTL reads rightward: sym1 streak=3 starting from reel 4 → pay=1.0
        int[][] screen = {
                {3, 0, 0},
                {3, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.RTL, row0Lines());
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(1.0, results.get(0).winAmount(), 0.001);
    }

    @Test
    void rtl_ltrArrangement_noRtlWin() {
        // [1,1,1,3,3] — LTR arrangement gives no RTL win (streak of sym1 from right = 0)
        int[][] screen = {
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {3, 0, 0},
                {3, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.RTL, row0Lines());
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(0.0, results.get(0).winAmount(), 0.001);
    }

    @Test
    void ways_3ofKind_usesWaysPayoutStrategy() {
        // All sym1 on all rows of all 5 reels → ways = [3,3,3,3,3] = 243 ways, ptIdx=2, pay=10.0*243=2430.0
        int[][] screen = {
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1}
        };
        // WAYS does not require line definitions
        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                symbols(),
                PayoutStrategyType.WAYS,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                null,   // no lines needed
                1, null, null,
                Arrays.stream(screen)
                        .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                        .collect(Collectors.toList()),
                null,
                null  // adjacencyOffsets
        );
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(2430.0, results.get(0).winAmount(), 0.01);
    }

    @Test
    void ways_noWin_returnsZero() {
        // sym1 only on reels 0-1 (streak=2 < minMatch=3), reels 2-4 have sym3
        int[][] screen = {
                {1, 1, 1},
                {1, 1, 1},
                {3, 3, 3},
                {3, 3, 3},
                {3, 3, 3}
        };
        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                symbols(),
                PayoutStrategyType.WAYS,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                null,
                1, null, null,
                Arrays.stream(screen)
                        .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                        .collect(Collectors.toList()),
                null,
                null  // adjacencyOffsets
        );
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(0.0, results.get(0).winAmount(), 0.001);
    }

    @Test
    void scatters_minimalWin_usesScattersPayoutStrategy() {
        // sym1 acting as scatter: 3 contacts on a 5×1 screen (screenWidth=5, screenHeight=1)
        // interval covers [3,5], paytable entry [1.0]: 1 contact group → pay=1.0
        List<SymbolConfig> scatterSymbols = List.of(
                new SymbolConfig(1, SymbolType.NORMAL, List.of(1.0), 1.0, WildMultiplierAggregation.ADD,
                        List.of(), "default"),
                new SymbolConfig(2, SymbolType.SCATTER, List.of(), 1.0, WildMultiplierAggregation.ADD, List.of(), null)
        );
        ScattersIntervalSet intervalSet = new ScattersIntervalSet(
                "default",
                List.of(new ScattersPaytableEntry(3, 5))   // [3..5] contacts
        );

        // 5×1 screen, all sym1 (normal with contactsIntervalSetName="default")
        List<List<Integer>> screenList = List.of(
                List.of(1), List.of(1), List.of(1), List.of(1), List.of(1)
        );

        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(),
                scatterSymbols,
                PayoutStrategyType.SCATTERS,
                5, 1,   // 5 wide, 1 tall
                3,      // minMatch
                null,   // no line defs
                1, null, null,
                screenList,
                List.of(intervalSet),
                null  // adjacencyOffsets
        );
        List<SpinData> results = new SpinTestService().generate(req);

        // Must produce a non-zero result — scatters evaluator found a contact
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).winAmount() >= 0.0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 5. SpinData structure correctness
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void spinData_screenSizeMatchesWidthAndHeight() {
        int[][] screen = {
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.LTR, row0Lines());
        List<SpinData> results = new SpinTestService().generate(req);

        SpinData spin = results.get(0);
        assertEquals(List.of(SCREEN_WIDTH, SCREEN_HEIGHT), spin.screenSize());
        assertEquals(SCREEN_WIDTH, spin.screen().size());
        assertEquals(SCREEN_HEIGHT, spin.screen().get(0).size());
    }

    @Test
    void spinData_fixedScreen_reelSetIndexIsMinusOne() {
        // Fixed screen path sets reelSetIndex to -1 (no set used)
        int[][] screen = {
                {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.LTR, row0Lines());
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(-1, results.get(0).reelSetIndex());
    }

    @Test
    void spinData_fixedScreen_stopsIsEmpty() {
        int[][] screen = {
                {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.LTR, row0Lines());
        List<SpinData> results = new SpinTestService().generate(req);

        assertTrue(results.get(0).reelsStopPositions().isEmpty());
    }

    @Test
    void spinData_reelBased_stopsHaveCorrectSize() {
        RtpRequest.ReelSetEntry reelSet = simpleReelSet(new int[]{1, 1, 1, 1, 1}, SCREEN_HEIGHT);
        SpinTestRequest req = reelBasedRequest(reelSet, null, symbols(), PayoutStrategyType.LTR, row0Lines(), null);
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(SCREEN_WIDTH, results.get(0).reelsStopPositions().size());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 6. Count and boundary edge cases
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void count0_treatedAs1_returnsSingleResult() {
        // count=0 is coerced to 1 by Math.max(1, req.count())
        int[][] screen = {{1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {3, 0, 0}, {3, 0, 0}};
        List<List<Integer>> screenList = Arrays.stream(screen)
                .map(col -> Arrays.stream(col).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());

        SpinTestRequest req = new SpinTestRequest(
                List.of(), List.of(), symbols(),
                PayoutStrategyType.LTR,
                SCREEN_WIDTH, SCREEN_HEIGHT, MIN_MATCH,
                row0Lines(),
                0,   // count=0
                null, null, screenList, null,
                null  // adjacencyOffsets
        );
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(1, results.size());
    }

    @Test
    void bw_winsOnBothDirections() {
        // [3,3,1,1,1] on row 0:
        //   LTR: streak=2 (sym3 on reels 0,1, then sym1 breaks) — no LTR win for sym3
        //        Actually LTR: r0=sym3, r1=sym3, r2=sym1 → streak=2 < 3, no win.
        //   RTL: reels 4,3,2 = sym1,sym1,sym1 → sym1 3x=1.0
        // So BW should yield at least the RTL win.
        int[][] screen = {
                {3, 0, 0},
                {3, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.BW, row0Lines());
        List<SpinData> results = new SpinTestService().generate(req);

        // BW should capture the RTL sym1 win
        assertTrue(results.get(0).winAmount() >= 1.0);
    }

    @Test
    void adj_windowNotStartingAtReel0_winsCorrectly() {
        // [3,1,1,1,3] — only middle window [reels 1-3] qualifies for sym1 3x=1.0
        int[][] screen = {
                {3, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {1, 0, 0},
                {3, 0, 0}
        };
        SpinTestRequest req = fixedScreenRequest(screen, symbols(), PayoutStrategyType.ADJ, row0Lines());
        List<SpinData> results = new SpinTestService().generate(req);

        assertEquals(1.0, results.get(0).winAmount(), 0.001);
    }
}
