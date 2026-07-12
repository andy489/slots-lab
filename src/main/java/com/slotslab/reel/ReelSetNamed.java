package com.slotslab.reel;

import java.util.List;

public record ReelSetNamed(
        String setName,
        List<List<Integer>> reelSet
) {}
