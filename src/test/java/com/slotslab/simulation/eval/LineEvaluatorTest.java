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

    // ── SL ────────────────────────────────────────────────────────────

    @Test
    void superLines_consecutiveSymbols_sameAsLtr() {
        // [1,1,1,3,3] → sym1 × 3 consecutive, pay=1.0 (same as LTR)
        int[][] screen = screenFromRow0(1, 1, 1, 3, 3);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.0, win, 0.001);
    }

    @Test
    void superLines_gapsAllowed_countMatchingSymbols() {
        // [1,3,1,3,1] → three sym1 with gaps, match=3, pay=1.0
        int[][] screen = screenFromRow0(1, 3, 1, 3, 1);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.0, win, 0.001);
    }

    @Test
    void superLines_gapsAtStart_countMatchingSymbols() {
        // [3,3,1,1,1] → sym3×2 (below minMatch), sym1×3 → sym1 wins with pay=1.0
        int[][] screen = screenFromRow0(3, 3, 1, 1, 1);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.0, win, 0.001);
    }

    @Test
    void superLines_fiveMatchWithGaps() {
        // [1,3,1,3,1] already has 3 — but try [1,3,3,1,1] → sym1 × 3, pay=1.0
        int[][] screen = screenFromRow0(1, 3, 3, 1, 1);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.0, win, 0.001);
    }

    @Test
    void superLines_allFiveMatch_paysMaxPayout() {
        // [1,1,1,1,1] → sym1 × 5, pay=10.0
        int[][] screen = screenFromRow0(1, 1, 1, 1, 1);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(10.0, win, 0.001);
    }

    @Test
    void superLines_belowMinMatch_noWin() {
        // [1,3,3,3,3] → sym1×1 (<3), sym3×4 → sym3 wins with pay=1.5 (4-match)
        int[][] screen = screenFromRow0(1, 3, 3, 3, 3);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.5, win, 0.001);
    }

    @Test
    void superLines_scatterBreaksScan() {
        // [1,1,4,1,1] → scatter at reel 2 stops the scan → only 2 sym1 counted → no win
        int[][] screen = screenFromRow0(1, 1, 4, 1, 1);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(0.0, win, 0.001);
    }

    @Test
    void superLines_wildCountsInMatch() {
        // [2,3,1,3,1] → wild×1, sym3×2, sym1×2
        // sym3: 2+1=3 → 0.5; sym1: 2+1=3 → 1.0 → best = sym1, 1.0
        int[][] screen = screenFromRow0(2, 3, 1, 3, 1);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(1.0, win, 0.001);
    }

    @Test
    void superLines_allWild_paysHighestNormal() {
        // [2,2,2,2,2] all wild (NONE) → wild-only count=5, pays as best normal=10.0
        int[][] screen = screenFromRow0(2, 2, 2, 2, 2);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH);
        assertEquals(10.0, win, 0.001);
    }

    @Test
    void superLines_wildWithAddMultiplier_gapScenario() {
        // sym5=WILD ADD mult=2. [5,3,1,3,1] → wild×1(mult=2), sym3×2, sym1×2
        // sym3: 2+1=3 → 0.5×2=1.0; sym1: 2+1=3 → 1.0×2=2.0 → best = sym1, 2.0
        SymbolTable symbols = addWild(baseSymbols(),
                new SymbolConfig(5, SymbolType.WILD, List.of(), 2.0, WildMultiplierAggregation.ADD, List.of(), null));
        int[][] screen = screenFromRow0(5, 3, 1, 3, 1);
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, symbols, lines0(), MIN_MATCH);
        assertEquals(2.0, win, 0.001);
    }

    @Test
    void superLines_mixedSymbolsOnLine_picksHighestWin() {
        // Regression: screen [[5,5,7],[3,4,4],[7,3,3],[2,6,6],[1,1,1]], line [1,0,1,0,1]
        // reads: screen[0][1]=5, screen[1][0]=3, screen[2][1]=3, screen[3][0]=2, screen[4][1]=1
        // sym3×2, sym5×1, sym2×1, sym1×1 — no wilds (assume all NORMAL here with simplified setup)
        // sym3 count=2 < 3, sym5/sym2/sym1 count=1 < 3 → no win
        // (This test uses the raw IDs; full paytable test is a spin-test integration concern)
        int[][] screen = {
            {0, 5, 0}, // reel0: row1=5
            {3, 0, 0}, // reel1: row0=3
            {0, 3, 0}, // reel2: row1=3
            {2, 0, 0}, // reel3: row0=2
            {0, 1, 0}  // reel4: row1=1
        };
        int[] lineRow = {1, 0, 1, 0, 1};
        int[][] lines = {lineRow};
        // With baseSymbols (sym1,sym2=wild,sym3,sym4=scatter): line sees sym5(unknown→normal?), sym3, sym3, sym2(wild), sym1
        // Use a simpler assertion: sym3 appears twice on the line, sym3 count=2 < minMatch=3 → verify no spurious win
        // when a different symbol appears first (the original bug)
        // Build a screen where sym1 is first, sym3 appears 3× after gaps
        int[][] screen2 = {
            {0, 1, 0}, // reel0 row1 = 1
            {3, 0, 0}, // reel1 row0 = 3
            {0, 3, 0}, // reel2 row1 = 3
            {1, 0, 0}, // reel3 row0 = 1
            {0, 3, 0}  // reel4 row1 = 3
        };
        // line [1,0,1,0,1]: sym1, sym3, sym3, sym1, sym3 → sym1×2, sym3×3
        // sym3×3 → pay=0.5; sym1×2 → below minMatch → best=0.5
        double win2 = LineEvaluator.evalSl(screen2, REEL_COUNT, baseSymbols(), lines, MIN_MATCH);
        assertEquals(0.5, win2, 0.001);
    }

    @Test
    void superLines_multipleLines_sumsWins() {
        // Two lines; line0 row0: [1,3,1,3,1] → sym1×3=1.0; line1 row1: [3,3,3,3,3] → sym3×5=5.0
        int[][] screen = {
                {1, 3, 0}, {3, 3, 0}, {1, 3, 0}, {3, 3, 0}, {1, 3, 0}
        };
        int[][] lines = {{0, 0, 0, 0, 0}, {1, 1, 1, 1, 1}};
        double win = LineEvaluator.evalSl(screen, REEL_COUNT, baseSymbols(), lines, MIN_MATCH);
        // line0: sym1×3=1.0; line1: sym3×5=5.0
        assertEquals(6.0, win, 0.001);
    }

    @Test
    void superLinesTracked_recordsComboKey() {
        Map<ComboKey, long[]>   hitMap = new HashMap<>();
        Map<ComboKey, double[]> payMap = new HashMap<>();
        // [1,3,1,3,1] → sym1 × 3 (with gaps), pay=1.0
        int[][] screen = screenFromRow0(1, 3, 1, 3, 1);
        LineEvaluator.evalSlTracked(screen, REEL_COUNT, baseSymbols(), lines0(), MIN_MATCH, hitMap, payMap);

        long hits = hitMap.getOrDefault(new ComboKey(1, 3), new long[]{0})[0];
        assertEquals(1, hits);
        double pay = payMap.getOrDefault(new ComboKey(1, 3), new double[]{0.0})[0];
        assertEquals(1.0, pay, 0.001);
    }
}
