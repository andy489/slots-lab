package com.slotslab.simulation.strategy;

import com.slotslab.simulation.config.SymbolTable;
import com.slotslab.simulation.eval.LineEvaluator;

public class LtrPayoutStrategy implements PayoutStrategy {
    @Override
    public double evaluate(int[][] screen, int screenWidth, SymbolTable symbols, int[][] lines, int minMatch) {
        return LineEvaluator.evalLtr(screen, screenWidth, symbols, lines, minMatch);
    }
}
