package com.slotslab.agent.skills.ltr;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces {@code minMatch} no-win reel sets via the spiral zero-placement method.
 * Each reel set guarantees no LTR win of length >= minMatch by zeroing exactly
 * one reel per symbol within the first {@code minMatch} reels, breaking every possible
 * reel-0-anchored winning chain regardless of where on the strip the window lands.
 *
 * <p>The spiral break pattern {@code (phase + symIdx) % minMatch} has exactly
 * {@code minMatch} distinct phases before it repeats, so exactly {@code minMatch}
 * distinct no-win sets exist — generating more would only duplicate them. The set
 * count is therefore fixed at {@code minMatch} (clamped to the reel count).
 */
@Component
public class SpiralNoWinReelSetFactory {

    /**
     * @param baseCounts  per-symbol tile counts for a full (winning) reel
     * @param screenWidth number of reels per set
     * @param minMatch    the win length to break; also the number of distinct no-win
     *                    sets produced. Break reels land within [0, minMatch-1].
     * @return list of {@code min(minMatch, screenWidth)} reel sets; each is
     *         int[screenWidth][numSymbols]
     */
    public List<int[][]> create(int[] baseCounts, int screenWidth, int minMatch) {
        List<int[][]> reelSets = new ArrayList<>();
        int numSymbols = baseCounts.length;
        // The break window can never exceed the reel count, and must be >= 1.
        // It doubles as the phase count: there are exactly this many distinct spirals.
        int breakWindow = Math.max(1, Math.min(minMatch, screenWidth));

        for (int phase = 0; phase < breakWindow; phase++) {
            int[][] reelSet = new int[screenWidth][numSymbols];
            for (int r = 0; r < screenWidth; r++) {
                System.arraycopy(baseCounts, 0, reelSet[r], 0, numSymbols);
            }
            // For each symbol, zero one break reel within [0, minMatch-1] so that
            // no minMatch consecutive reels starting at reel 0 can all be non-zero.
            for (int symIdx = 0; symIdx < numSymbols; symIdx++) {
                int breakReel = (phase + symIdx) % breakWindow;
                reelSet[breakReel][symIdx] = 0;
            }
            reelSets.add(reelSet);
        }
        return reelSets;
    }
}
