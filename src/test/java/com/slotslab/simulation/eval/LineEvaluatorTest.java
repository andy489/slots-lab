package com.slotslab.simulation.eval;

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
 * Screen layout (screen[reel][row]):
 *   minMatch=3, reelCount=5, screenHeight=3
 *
 *   sym 1  NORMAL  paytable [1.0, 3.0, 10.0]
 *   sym 2  WILD    aggregation NONE
 *   sym 3  NORMAL  paytable [0.5, 1.5,  5.0]
 *   sym 4  SCATTER
 *   sym 5  WILD    aggregation ADD   multiplier=2.0
 *   sym 6  WILD    aggregation MULTIPLY multiplier=2.0
 *   sym 7  WILD    aggregation SEQUENCE sequence=[2.0, 4.0, 8.0]
 */
class LineEvaluatorTest {

    private static final int MIN_MATCH    = 3;
    private static final int REEL_COUNT   = 5;

    private static SymbolTable baseSymbols() {
        return new SymbolTable(List.of(
                SymbolConfig.normal(1, List.of(1.0, 3.0, 10.0)),
                new SymbolConfig(2, SymbolType.WILD, List.of(), 1.0, WildMultiplierAggregation.NONE, List.of(), null),
                SymbolConfig.normal(3, List.of(0.5, 1.5, 5.0)),
                new SymbolConfig(4, SymbolType.SCATTER, List.of(), 0.0, WildMultiplierAggregation.NONE, List.of(), null)
        ));
    }

    private static SymbolTable addWild(SymbolTable base, SymbolConfig wildCfg) {
        List<SymbolConfig> all = new java.util.ArrayList<>(base.all());
        all.add(wildCfg);
        return new SymbolTable(all);
    }

    // ── single-line convenience: line=[0,0,0,0,0] (row 0 on every reel) ──────

    private static int[][] screenFromRow0(int... symsPerReel) {
        int[][] s = new int[symsPerReel.length][3];
        for (int r = 0; r < symsPerReel.length; r++) s[r][0] = symsPerReel[r];
        return s;
    }

    private static int[][] lines0() {
        return new int[][] {{0, 0, 0, 0, 0}};
    }

    // ── LTR basic wins ───────────────────────────────────────────────────────

    @Test
    void ltr_3ofKind_noWild() {
        // [1,1,1,3,3] → sym1 streak=3, pay=1.0
        int[][] screen = screenFromRow0(1, 1, 1, 3, 3);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.0, win, 0.001);
    }

    @Test
    void ltr_5ofKind_noWild() {
        // [1,1,1,1,1] → streak=5, ptIdx=2, pay=10.0
        int[][] screen = screenFromRow0(1, 1, 1, 1, 1);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(10.0, win, 0.001);
    }

    @Test
    void ltr_belowMinMatch_noWin() {
        // [1,1,3,3,3] → streak=2 < minMatch
        int[][] screen = screenFromRow0(1, 1, 3, 3, 3);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(0.0, win, 0.001);
    }

    @Test
    void ltr_scatterAtStart_noWin() {
        // scatter at reel 0 → no win at all
        int[][] screen = screenFromRow0(4, 1, 1, 1, 1);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(0.0, win, 0.001);
    }

    @Test
    void ltr_scatterBreaksStreak() {
        // [1,1,4,1,1] → scatter at reel 2 breaks streak=2 < minMatch
        int[][] screen = screenFromRow0(1, 1, 4, 1, 1);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(0.0, win, 0.001);
    }

    // ── LTR wild substitution ─────────────────────────────────────────────────

    @Test
    void ltr_wildSubstitutes_formsMinMatch() {
        // [2,1,1,3,3] → wild+sym1+sym1 = streak=3, pay=1.0
        int[][] screen = screenFromRow0(2, 1, 1, 3, 3);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.0, win, 0.001);
    }

    @Test
    void ltr_allWild_paysHighestNormal() {
        // [2,2,2,2,2] all wild (NONE aggregation) → wild-only streak=5, pays as best normal=10.0
        int[][] screen = screenFromRow0(2, 2, 2, 2, 2);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(10.0, win, 0.001);
    }

    @Test
    void ltr_wildLeadsNormalFollows_streakIsFull() {
        // [2,2,1,1,1] → wild at 0,1 then sym1 at 2,3,4 → streak=5, paySymbol=1, pay=10.0
        int[][] screen = screenFromRow0(2, 2, 1, 1, 1);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(10.0, win, 0.001);
    }

    // ── RTL ───────────────────────────────────────────────────────────────────

    @Test
    void rtl_3ofKind_rightToLeft() {
        // [3,3,1,1,1] → RTL reads right: 1,1,1,3,3 → sym1 streak=3, pay=1.0
        int[][] screen = screenFromRow0(3, 3, 1, 1, 1);
        double win = LineEvaluator.evalRtl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.0, win, 0.001);
    }

    @Test
    void rtl_noWin_whenSymIsOnLeft() {
        // [1,1,1,3,3] → RTL reads right: 3,3,1,1,1 → streak=2 < 3 (only sym3 qualifies from right)
        int[][] screen = screenFromRow0(1, 1, 1, 3, 3);
        double win = LineEvaluator.evalRtl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(0.0, win, 0.001);
    }

    // ── ADJ ───────────────────────────────────────────────────────────────────

    @Test
    void adj_middleWindow() {
        // [3,1,1,1,3] → best window is reels 1-3, sym1 streak=3, pay=1.0
        int[][] screen = screenFromRow0(3, 1, 1, 1, 3);
        double win = LineEvaluator.evalAdj(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.0, win, 0.001);
    }

    @Test
    void adj_picksHighestWindow() {
        // [3,1,1,1,1] → windows: r1-3=3x(1.0), r1-4=4x(3.0), r1-5=5x(10.0)?
        // Actually: starts from r0: [3,1,1,1,1] → r0 sym3 streak breaks after checking r0 start
        // start=0: sym3, streak limited; start=1: sym1×4=3.0; check all windows
        int[][] screen = screenFromRow0(3, 1, 1, 1, 1);
        double win = LineEvaluator.evalAdj(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(3.0, win, 0.001); // sym1 × 4 = ptIdx=1 → 3.0
    }

    // ── Wild multiplier: ADD ──────────────────────────────────────────────────

    @Test
    void ltr_wildAdd_oneWild_doublesMultiplier() {
        // sym5=WILD ADD mult=2. [5,1,1,3,3] → streak=3, paySymbol=1, multiplier=2, win=1.0*2=2.0
        SymbolTable symbols = addWild(baseSymbols(),
                new SymbolConfig(5, SymbolType.WILD, List.of(), 2.0, WildMultiplierAggregation.ADD, List.of(), null));
        int[][] screen = screenFromRow0(5, 1, 1, 3, 3);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, symbols, lines0(), MIN_MATCH);
        assertEquals(2.0, win, 0.001);
    }

    @Test
    void ltr_wildAdd_twoWilds_addsMultipliers() {
        // [5,5,1,1,1] → streak=5, multiplier=2+2=4, win=10.0*4=40.0
        SymbolTable symbols = addWild(baseSymbols(),
                new SymbolConfig(5, SymbolType.WILD, List.of(), 2.0, WildMultiplierAggregation.ADD, List.of(), null));
        int[][] screen = screenFromRow0(5, 5, 1, 1, 1);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, symbols, lines0(), MIN_MATCH);
        assertEquals(40.0, win, 0.001);
    }

    // ── Wild multiplier: MULTIPLY ─────────────────────────────────────────────

    @Test
    void ltr_wildMultiply_twoWilds_multipliesMultipliers() {
        // sym6=WILD MULTIPLY mult=2. [6,6,1,1,1] → streak=5, multiplier=2*2=4, win=10.0*4=40.0
        SymbolTable symbols = addWild(baseSymbols(),
                new SymbolConfig(6, SymbolType.WILD, List.of(), 2.0, WildMultiplierAggregation.MULTIPLY, List.of(), null));
        int[][] screen = screenFromRow0(6, 6, 1, 1, 1);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, symbols, lines0(), MIN_MATCH);
        assertEquals(40.0, win, 0.001);
    }

    // ── Wild multiplier: SEQUENCE ─────────────────────────────────────────────

    @Test
    void ltr_wildSequence_oneWild_usesFirstIndex() {
        // sym7=WILD SEQUENCE [2.0, 4.0, 8.0]. [7,1,1,3,3] → 1 wild → seq[0]=2.0, win=1.0*2=2.0
        SymbolTable symbols = addWild(baseSymbols(),
                new SymbolConfig(7, SymbolType.WILD, List.of(), 0.0, WildMultiplierAggregation.SEQUENCE,
                        List.of(2.0, 4.0, 8.0), null));
        int[][] screen = screenFromRow0(7, 1, 1, 3, 3);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, symbols, lines0(), MIN_MATCH);
        assertEquals(2.0, win, 0.001);
    }

    @Test
    void ltr_wildSequence_twoWilds_usesSecondIndex() {
        // [7,7,1,1,1] → 2 wilds → seq[1]=4.0, win=10.0*4=40.0
        SymbolTable symbols = addWild(baseSymbols(),
                new SymbolConfig(7, SymbolType.WILD, List.of(), 0.0, WildMultiplierAggregation.SEQUENCE,
                        List.of(2.0, 4.0, 8.0), null));
        int[][] screen = screenFromRow0(7, 7, 1, 1, 1);
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, symbols, lines0(), MIN_MATCH);
        assertEquals(40.0, win, 0.001);
    }

    // ── Tracked stats ─────────────────────────────────────────────────────────

    @Test
    void ltrTracked_recordsComboKey() {
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();
        int[][] screen = screenFromRow0(1, 1, 1, 3, 3);
        LineEvaluator.evalLtrTracked(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH, hitMap, payMap);

        long hits = hitMap.getOrDefault(new ComboKey(1, 3), new long[]{0})[0];
        assertEquals(1, hits);
        double pay = payMap.getOrDefault(new ComboKey(1, 3), new double[]{0.0})[0];
        assertEquals(1.0, pay, 0.001);
    }

    @Test
    void rtlTracked_recordsCorrectStreak() {
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();
        // RTL: [3,3,1,1,1] → sym1 streak=3 from the right
        int[][] screen = screenFromRow0(3, 3, 1, 1, 1);
        LineEvaluator.evalRtlTracked(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH, hitMap, payMap);

        long hits = hitMap.getOrDefault(new ComboKey(1, 3), new long[]{0})[0];
        assertEquals(1, hits);
    }

    @Test
    void adjTracked_recordsHighestWindow() {
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();
        int[][] screen = screenFromRow0(3, 1, 1, 1, 3);
        LineEvaluator.evalAdjTracked(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH, hitMap, payMap);

        long hits = hitMap.getOrDefault(new ComboKey(1, 3), new long[]{0})[0];
        assertEquals(1, hits);
    }

    // ── Multiple lines ────────────────────────────────────────────────────────

    @Test
    void ltr_multipleLines_sumsWins() {
        // line 0: row 0 all. line 1: row 1 all.
        int[][] screen = {
                {1, 3, 0}, {1, 3, 0}, {1, 3, 0}, {3, 1, 0}, {3, 1, 0}
        };
        int[][] lines = {{0, 0, 0, 0, 0}, {1, 1, 1, 1, 1}};
        double win = LineEvaluator.evalLtr(screen, REEL_COUNT, baseSymbols(), lines, MIN_MATCH);
        // line0: sym1 streak=3, pay=1.0; line1: sym3 streak=3, pay=0.5 → total=1.5
        assertEquals(1.5, win, 0.001);
    }
}
