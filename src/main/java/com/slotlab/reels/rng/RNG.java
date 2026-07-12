package com.slotlab.reels.rng;

import java.util.List;
import java.util.random.RandomGenerator;

public class RNG implements IRNG {
    private final RandomGenerator rg;

    public RNG() {
        this.rg = RandomGenerator.of("Xoshiro256PlusPlus");
    }

    RNG(RandomGenerator rg) {
        this.rg = rg;
    }

    @Override
    public int getRandInRange(int l, int r) {
        return rg.nextInt(l, r);
    }

    @Override
    public double getDouble(double l, double r) {
        return rg.nextDouble(l, r);
    }

    @Override
    public int getWeightedRand(List<Integer> outcomes, List<Double> chances) {
        if (outcomes.size() != chances.size()) {
            throw new IllegalArgumentException("outcomes and chances must have the same size");
        }
        double total = 0.0;
        var bag = new java.util.TreeMap<Double, Integer>();
        for (int i = 0; i < outcomes.size(); i++) {
            double w = chances.get(i);
            if (w > 0.0) {
                total += w;
                bag.put(total, outcomes.get(i));
            }
        }
        if (total == 0.0) throw new IllegalArgumentException("all chances are zero");
        return bag.higherEntry(rg.nextDouble(0.0, total)).getValue();
    }
}
