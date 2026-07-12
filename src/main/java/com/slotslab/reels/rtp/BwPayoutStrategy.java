package com.slotslab.reels.rtp;

public class BwPayoutStrategy implements PayoutStrategy {
    @Override
    public double evaluate(int[][] screen, int screenWidth, SymbolTable symbols, int[][] lines, int minMatch) {
        double ltr = LineEvaluator.evalLtr(screen, screenWidth, symbols, lines, minMatch);
        double rtl = LineEvaluator.evalRtl(screen, screenWidth, symbols, lines, minMatch);
        return ltr + rtl;
    }
}
