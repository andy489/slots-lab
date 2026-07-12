package org.evo.reels.reel;

import java.util.List;

public record ReelSetEvo(
        String setName,
        List<List<Integer>> reelSet
) {}
