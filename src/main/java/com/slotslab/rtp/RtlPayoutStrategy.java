package com.slotslab.rtp;

public class RtlPayoutStrategy implements PayoutStrategy {
    @Override
    public double evaluate(int[][] screen, int screenWidth, SymbolTable symbols, int[][] lines, int minMatch) {
        return LineEvaluator.evalRtl(screen, screenWidth, symbols, lines, minMatch);
    }
}
