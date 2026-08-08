package com.slotslab.agent.skills.ltr;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces screenWidth no-win reel sets via the spiral zero-placement method.
 * Each reel set guarantees no LTR win of length >= minMatch by zeroing exactly
 * one reel per symbol within the first minMatch reels, breaking every possible
 * winning chain regardless of where on the strip the window lands.
 */
@Component
public class SpiralNoWinReelSetFactory {

    /**
     * @return list of screenWidth reel sets; each is int[screenWidth][numSymbols]
     */
    public List<int[][]> create(int[] baseCounts, int screenWidth, int minMatch) {
        List<int[][]> reelSets = new ArrayList<>();
        int numSymbols = baseCounts.length;

        for (int phase = 0; phase < minMatch; phase++) {
            int[][] reelSet = new int[screenWidth][numSymbols];
            for (int r = 0; r < screenWidth; r++) {
                System.arraycopy(baseCounts, 0, reelSet[r], 0, numSymbols);
            }
            // For each symbol, zero the break reel within [0, minMatch-1] so that
            // no minMatch consecutive reels can all be non-zero for that symbol.
            for (int symIdx = 0; symIdx < numSymbols; symIdx++) {
                int breakReel = (phase + symIdx) % minMatch;
                reelSet[breakReel][symIdx] = 0;
            }
            reelSets.add(reelSet);
        }
        return reelSets;
    }
}
