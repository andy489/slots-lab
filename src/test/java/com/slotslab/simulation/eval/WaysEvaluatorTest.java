package com.slotslab.simulation.eval;

import com.slotslab.dto.ways.WayLineDto;
import com.slotslab.dto.ways.WayLinesDto;
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
 * Symbols used across all tests (minMatch = 3, screenWidth = 5):
 *   sym 1  NORMAL  paytable [1.0, 3.0, 10.0]  (3x, 4x, 5x)
 *   sym 2  WILD    aggregation NONE
 *   sym 3  NORMAL  paytable [0.2, 0.6,  2.0]
 *   sym 4  NORMAL  paytable [2.0, 6.0, 20.0]
 *   sym 5  NORMAL  paytable [3.0, 9.0, 30.0]
 *   sym 6  NORMAL  paytable [0.1, 0.3,  1.0]
 *   sym 7  NORMAL  paytable [0.5, 1.5,  5.0]
 */
class WaysEvaluatorTest {

    private static final int MIN_MATCH    = 3;
    private static final int SCREEN_WIDTH = 5;
    private static final int SCREEN_HEIGHT = 3;

    // ── symbol table ─────────────────────────────────────────────────────────

    private static SymbolTable symbols() {
        return new SymbolTable(List.of(
            SymbolConfig.normal(1, List.of(1.0, 3.0, 10.0)),
            new SymbolConfig(2, SymbolType.WILD, List.of(), 1.0, WildMultiplierAggregation.NONE, List.of()),
            SymbolConfig.normal(3, List.of(0.2, 0.6,  2.0)),
            SymbolConfig.normal(4, List.of(2.0, 6.0, 20.0)),
            SymbolConfig.normal(5, List.of(3.0, 9.0, 30.0)),
            SymbolConfig.normal(6, List.of(0.1, 0.3,  1.0)),
            SymbolConfig.normal(7, List.of(0.5, 1.5,  5.0))
        ));
    }

    // screen[reel][row]
    private static int[][] screen1() {
        // [[7,2,2],[5,2,6],[4,5,5],[3,5,6],[4,7,7]]
        return new int[][] {
            {7, 2, 2},
            {5, 2, 6},
            {4, 5, 5},
            {3, 5, 6},
            {4, 7, 7}
        };
    }

    private static int[][] screen2() {
        // [[2,1,1],[2,1,1],[2,1,1],[1,1,1],[1,1,1]]
        return new int[][] {
            {2, 1, 1},
            {2, 1, 1},
            {2, 1, 1},
            {1, 1, 1},
            {1, 1, 1}
        };
    }

    private static int[][] screen3() {
        // [[2,1,1],[2,2,1],[2,2,2],[4,1,1],[1,1,1]]
        return new int[][] {
            {2, 1, 1},
            {2, 2, 1},
            {2, 2, 2},
            {4, 1, 1},
            {1, 1, 1}
        };
    }

    // ── Screen 1: [[7,2,2],[5,2,6],[4,5,5],[3,5,6],[4,7,7]] ─────────────────
    //
    // sym5 win:
    //   r0=[2,2]→wilds at rows1,2 → positions [1,2]
    //   r1=[5,2,6]→sym5 row0, wild row1 → [0,1]
    //   r2=[4,5,5]→sym5 rows1,2 → [1,2]
    //   r3=[3,5,6]→sym5 row1 → [1]
    //   r4=[4,7,7]→none → broke, streak=4
    //   ways=[2,2,2,1], totalSimpleLines=8, ptIdx=4-3=1, singularPay=9.0, win=9.0*8=72.0
    //
    // sym4 win:
    //   r0→wilds [1,2]
    //   r1=[5,2,6]→wild row1 → [1]
    //   r2=[4,5,5]→sym4 row0 → [0]
    //   r3=[3,5,6]→none → broke, streak=3
    //   containsAtLeastOneNormal: r2 has screen[2][0]=4 ✓
    //   ways=[2,1,1], totalSimpleLines=2, ptIdx=0, singularPay=2.0, win=2.0*2=4.0
    //
    // sym7: r0 has sym7 row0, r1 has no 7/wild at row0(=5)... r1 has no sym7
    //   actually r0=[7,2,2]→sym7 row0, wild rows1,2 → positions for sym7: [0,1,2]
    //   r1=[5,2,6]→wild row1 → [1]
    //   r2=[4,5,5]→none → broke, streak=2 < minMatch=3. No win.
    //
    // No other symbols reach minMatch on consecutive reels from r0.

    @Test
    void screen1_sym5_waysWin() {
        WayLinesDto result = WaysEvaluator.evalWays(screen1(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH);

        assertNotNull(result);
        WayLineDto sym5Line = result.wayLines().stream()
            .filter(l -> l.payoutSymbolId() == 5)
            .findFirst().orElse(null);

        assertNotNull(sym5Line, "expected a win for symbol 5");
        assertEquals(4, sym5Line.lineSize());
        assertEquals(List.of(2, 2, 2, 1), sym5Line.ways());
        assertEquals(8, sym5Line.totalSimpleLines());
        assertEquals(9.0, sym5Line.singularPay(), 0.001);
        assertEquals(72.0, sym5Line.winAmount(), 0.01);
    }

    @Test
    void screen1_sym4_waysWin() {
        WayLinesDto result = WaysEvaluator.evalWays(screen1(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH);

        assertNotNull(result);
        WayLineDto sym4Line = result.wayLines().stream()
            .filter(l -> l.payoutSymbolId() == 4)
            .findFirst().orElse(null);

        assertNotNull(sym4Line, "expected a win for symbol 4");
        assertEquals(3, sym4Line.lineSize());
        assertEquals(List.of(2, 1, 1), sym4Line.ways());
        assertEquals(2, sym4Line.totalSimpleLines());
        assertEquals(2.0, sym4Line.singularPay(), 0.001);
        assertEquals(4.0, sym4Line.winAmount(), 0.01);
    }

    @Test
    void screen1_totalWin() {
        WayLinesDto result = WaysEvaluator.evalWays(screen1(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH);

        assertNotNull(result);
        // sym5=72.0 + sym4=4.0 = 76.0
        assertEquals(76.0, result.winAmount(), 0.01);
        assertEquals(2, result.wayLines().size());
    }

    @Test
    void screen1_tracked_simpleLineCounts() {
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();
        WaysEvaluator.evalWaysTracked(screen1(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH, hitMap, payMap);

        // sym5 × 4 reels → 8 simple lines
        long sym5Hits = hitMap.getOrDefault(new ComboKey(5, 4), new long[]{0})[0];
        assertEquals(8, sym5Hits);

        // sym4 × 3 reels → 2 simple lines
        long sym4Hits = hitMap.getOrDefault(new ComboKey(4, 3), new long[]{0})[0];
        assertEquals(2, sym4Hits);
    }

    // ── Screen 2: [[2,1,1],[2,1,1],[2,1,1],[1,1,1],[1,1,1]] ─────────────────
    //
    // sym1 win:
    //   r0=[2,1,1]→wild row0, sym1 rows1,2 → [0,1,2]
    //   r1=[2,1,1]→same → [0,1,2]
    //   r2=[2,1,1]→same → [0,1,2]
    //   r3=[1,1,1]→sym1 all rows → [0,1,2]
    //   r4=[1,1,1]→[0,1,2]
    //   ways=[3,3,3,3,3], totalSimpleLines=243, ptIdx=5-3=2, singularPay=10.0, win=2430.0
    //
    // wildOnlyClaimed=true (sym1 found) so no separate wild-only entry.

    @Test
    void screen2_sym1_fullStreak() {
        WayLinesDto result = WaysEvaluator.evalWays(screen2(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH);

        assertNotNull(result);
        assertEquals(1, result.wayLines().size());

        WayLineDto sym1Line = result.wayLines().get(0);
        assertEquals(1, sym1Line.payoutSymbolId());
        assertEquals(5, sym1Line.lineSize());
        assertEquals(List.of(3, 3, 3, 3, 3), sym1Line.ways());
        assertEquals(243, sym1Line.totalSimpleLines());
        assertEquals(10.0, sym1Line.singularPay(), 0.001);
        assertEquals(2430.0, sym1Line.winAmount(), 0.01);
    }

    @Test
    void screen2_tracked_243_simpleLines() {
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();
        WaysEvaluator.evalWaysTracked(screen2(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH, hitMap, payMap);

        long sym1Hits = hitMap.getOrDefault(new ComboKey(1, 5), new long[]{0})[0];
        assertEquals(243, sym1Hits);
    }

    // ── Screen 3: [[2,1,1],[2,2,1],[2,2,2],[4,1,1],[1,1,1]] ─────────────────
    //
    // sym1 win:
    //   r0=[2,1,1]→wild row0, sym1 rows1,2 → [0,1,2]
    //   r1=[2,2,1]→wild rows0,1, sym1 row2 → [0,1,2]
    //   r2=[2,2,2]→all wild → [0,1,2]
    //   r3=[4,1,1]→sym4 row0 (not 1, not wild), sym1 rows1,2 → [1,2]
    //   r4=[1,1,1]→[0,1,2]
    //   No break, size=5. containsAtLeastOneNormal: r0 has screen[0][1]=1 ✓
    //   ways=[3,3,3,2,3], totalSimpleLines=162, ptIdx=2, singularPay=10.0, win=1620.0
    //
    // sym4 win:
    //   r0→wilds [0,1,2]
    //   r1=[2,2,1]→wild rows0,1 → [0,1]
    //   r2=[2,2,2]→all wild → [0,1,2]
    //   r3=[4,1,1]→sym4 row0 → [0]
    //   r4=[1,1,1]→none → broke, size=4
    //   containsAtLeastOneNormal: r3 screen[3][0]=4 ✓
    //   ways=[3,2,3,1], totalSimpleLines=18, ptIdx=1, singularPay=6.0, win=108.0

    @Test
    void screen3_sym1_waysWin() {
        WayLinesDto result = WaysEvaluator.evalWays(screen3(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH);

        assertNotNull(result);
        WayLineDto sym1Line = result.wayLines().stream()
            .filter(l -> l.payoutSymbolId() == 1)
            .findFirst().orElse(null);

        assertNotNull(sym1Line);
        assertEquals(5, sym1Line.lineSize());
        assertEquals(List.of(3, 3, 3, 2, 3), sym1Line.ways());
        assertEquals(162, sym1Line.totalSimpleLines());
        assertEquals(10.0, sym1Line.singularPay(), 0.001);
        assertEquals(1620.0, sym1Line.winAmount(), 0.01);
    }

    @Test
    void screen3_sym4_waysWin() {
        WayLinesDto result = WaysEvaluator.evalWays(screen3(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH);

        assertNotNull(result);
        WayLineDto sym4Line = result.wayLines().stream()
            .filter(l -> l.payoutSymbolId() == 4)
            .findFirst().orElse(null);

        assertNotNull(sym4Line);
        assertEquals(4, sym4Line.lineSize());
        // r0=[2,1,1]→wild row0 only→[0]; r1→[0,1]; r2→[0,1,2]; r3=[4,1,1]→[0]
        assertEquals(List.of(1, 2, 3, 1), sym4Line.ways());
        assertEquals(6, sym4Line.totalSimpleLines());
        assertEquals(6.0, sym4Line.singularPay(), 0.001);
        assertEquals(36.0, sym4Line.winAmount(), 0.01);
    }

    @Test
    void screen3_totalWin() {
        WayLinesDto result = WaysEvaluator.evalWays(screen3(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH);

        assertNotNull(result);
        // sym1=1620.0 + sym4=36.0 = 1656.0
        assertEquals(1656.0, result.winAmount(), 0.01);
        assertEquals(2, result.wayLines().size());
    }

    @Test
    void screen3_tracked_simpleLineCounts() {
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();
        WaysEvaluator.evalWaysTracked(screen3(), SCREEN_WIDTH, SCREEN_HEIGHT, symbols(), MIN_MATCH, hitMap, payMap);

        long sym1Hits = hitMap.getOrDefault(new ComboKey(1, 5), new long[]{0})[0];
        assertEquals(162, sym1Hits);

        long sym4Hits = hitMap.getOrDefault(new ComboKey(4, 4), new long[]{0})[0];
        assertEquals(6, sym4Hits);
    }
}
