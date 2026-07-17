package com.slotslab.simulation.strategy;

import com.slotslab.dto.ways.WayLinesDto;
import com.slotslab.simulation.config.SymbolTable;
import com.slotslab.simulation.eval.WaysEvaluator;

public class MegawaysPayoutStrategy implements PayoutStrategy {

    @Override
    public double evaluate(int[][] screen, int screenWidth, SymbolTable symbols,
                           int[][] lines, int minMatch) {
        WayLinesDto result = WaysEvaluator.evalWays(screen, screenWidth, screen[0].length, symbols, minMatch);
        return result != null ? result.winAmount() : 0.0;
    }
}
