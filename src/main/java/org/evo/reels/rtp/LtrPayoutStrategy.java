package org.evo.reels.rtp;

public class LtrPayoutStrategy implements PayoutStrategy {
    @Override
    public double evaluate(int[][] screen, int screenWidth, SymbolTable symbols, int[][] lines, int minMatch) {
        return LineEvaluator.evalLtr(screen, screenWidth, symbols, lines, minMatch);
    }
}
