package com.slotslab.reels.rtp;

import java.util.List;

/**
 * Evaluates one spin on a set of paylines and returns the total win as a multiple of stake=1.
 *
 * screen[reel][row] = symbolId.
 * lines: each line is a list of row indices (one per reel, 0-based).
 */
public interface PayoutStrategy {
    double evaluate(int[][] screen, int screenWidth, SymbolTable symbols, int[][] lines, int minMatch);
}
