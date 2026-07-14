package com.slotslab.simulation.strategy;

import com.slotslab.simulation.config.SymbolTable;

public class ScattersPayoutStrategy implements PayoutStrategy {

    @Override
    public double evaluate(int[][] screen, int screenWidth, SymbolTable symbols,
                           int[][] lines, int minMatch) {
        throw new UnsupportedOperationException(
                "ScattersPayoutStrategy must be dispatched directly via ScattersEvaluator");
    }
}
