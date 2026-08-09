package com.slotslab.agent.skills.ltr;

import com.slotslab.reel.Restriction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill 06 — builds a Restriction (stack sizes, chances, min distance) for a reel set.
 * Max stack size = screenHeight + 2. Chances sum to 100.
 * No-win sets: shift mass left for lower hit rate.
 * Win sets: shift right for high volatility/low HR, shift left (smaller stacks) for high HR.
 * Min distance is 1 by default; increase only as a last resort (see skill doc).
 */
@Component
public class RestrictionBuilder {

    private static final double MAX_SHIFT     = 6.0;
    private static final double WIN_SHIFT_MAX = 8.0;  // low hit rate → large stacks
    private static final double WIN_SHIFT_MIN = -2.0; // high hit rate → smaller stacks

    public Restriction build(int screenHeight, double targetHitRate, boolean isWinSet) {
        int n = screenHeight + 2; // number of distinct stack sizes: 1 … n
        double[] base = baseVector(n);
        double[] chances;
        if (isWinSet) {
            // Higher hit rate → smaller stacks (shift left); lower → larger stacks (shift right).
            double t = (Math.max(5.0, Math.min(50.0, targetHitRate)) - 5.0) / 45.0;
            double winShift = WIN_SHIFT_MAX - t * (WIN_SHIFT_MAX - WIN_SHIFT_MIN);
            chances = winShift >= 0 ? shiftRight(base, winShift) : shiftLeft(base, targetHitRate);
        } else {
            chances = shiftLeft(base, targetHitRate);
        }
        normalise(chances);
        List<Integer> sizes = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) sizes.add(i);
        List<Double> chanceList = new ArrayList<>(n);
        for (double c : chances) chanceList.add(c);
        return new Restriction(sizes, chanceList, 1);
    }

    /** Bell-shaped base vector peaked near the middle index, sums to 100. */
    private static double[] baseVector(int n) {
        double[] v = new double[n];
        int peak = (n - 1) / 2; // 0-based peak index
        for (int i = 0; i < n; i++) {
            double dist = Math.abs(i - peak);
            v[i] = Math.max(1.0, 30.0 - dist * dist * 2.5);
        }
        normalise(v);
        return v;
    }

    /** Shift mass left for no-win sets: lower HR → stronger left shift. */
    private static double[] shiftLeft(double[] base, double targetHitRate) {
        int n = base.length;
        double t = (Math.max(5.0, Math.min(50.0, targetHitRate)) - 5.0) / 45.0;
        double amount = (1.0 - t) * MAX_SHIFT;
        double[] result = new double[n];
        double mid = (n - 1) / 2.0;
        for (int i = 0; i < n; i++) {
            double direction = mid - i; // positive for left half, negative for right half
            result[i] = Math.max(1.0, base[i] + amount * direction);
        }
        normalise(result);
        return result;
    }

    /** Shift mass right for win sets: always favour larger stacks. */
    private static double[] shiftRight(double[] base, double amount) {
        int n = base.length;
        double[] result = new double[n];
        double mid = (n - 1) / 2.0;
        for (int i = 0; i < n; i++) {
            double direction = i - mid; // positive for right half
            result[i] = Math.max(1.0, base[i] + amount * direction);
        }
        normalise(result);
        return result;
    }

    /** Scale array so it sums to exactly 100.0. */
    private static void normalise(double[] v) {
        double sum = 0;
        for (double x : v) sum += x;
        if (sum == 0) sum = 1;
        for (int i = 0; i < v.length; i++) v[i] = Math.round(v[i] / sum * 100.0 * 10.0) / 10.0;
        // fix rounding drift: adjust the peak bucket
        double total = 0;
        for (double x : v) total += x;
        double diff = Math.round((100.0 - total) * 10.0) / 10.0;
        if (diff != 0) {
            int peak = 0;
            for (int i = 1; i < v.length; i++) if (v[i] > v[peak]) peak = i;
            v[peak] = Math.max(1.0, v[peak] + diff);
        }
    }
}
