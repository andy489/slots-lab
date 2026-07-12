package com.slotlab.reels.reel;

import java.util.List;

public record ReelSetNamed(
        String setName,
        List<List<Integer>> reelSet
) {}
