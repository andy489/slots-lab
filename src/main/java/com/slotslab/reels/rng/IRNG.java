package com.slotslab.reels.rng;

import java.util.List;

public interface IRNG {
    /** @return random int in [l, r) */
    int getRandInRange(int l, int r);

    double getDouble(double l, double r);

    int getWeightedRand(List<Integer> outcomes, List<Double> chances);
}
