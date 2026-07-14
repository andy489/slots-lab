package com.slotslab.simulation.strategy;

import com.slotslab.simulation.config.SymbolTable;

public class ClustersPayoutStrategy implements PayoutStrategy {

    @Override
    public double evaluate(int[][] screen, int screenWidth, SymbolTable symbols,
                           int[][] lines, int minMatch) {
        throw new UnsupportedOperationException(
                "ClustersPayoutStrategy must be dispatched directly via ClustersEvaluator");
    }
}
