package com.slotslab.simulation.strategy;

import com.slotslab.simulation.config.SymbolTable;

public interface PayoutStrategy {
    double evaluate(int[][] screen, int screenWidth, SymbolTable symbols, int[][] lines, int minMatch);
}
