package com.slotslab.simulation.strategy;

import com.slotslab.simulation.config.SymbolTable;
import com.slotslab.simulation.eval.WaysEvaluator;

public class WaysPayoutStrategy implements PayoutStrategy {

    @Override
    public double evaluate(int[][] screen, int screenWidth, SymbolTable symbols,
                           int[][] lines, int minMatch) {
        return WaysEvaluator.evalWaysTracked(screen, screenWidth,
                screen[0].length, symbols, minMatch,
                new java.util.HashMap<>(), new java.util.HashMap<>());
    }
}
