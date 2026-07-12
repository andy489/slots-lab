package com.slotslab.reels.rtp;

/**
 * Selection chance for one reel set.
 * @param setIndex index into the generated reels array (0-based)
 * @param chance   probability weight in percent (0.000 – 100.000, summing to exactly 100.000)
 */
public record ReelSetChance(int setIndex, double chance) {}
