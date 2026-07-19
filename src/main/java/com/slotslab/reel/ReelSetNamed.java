package com.slotslab.reel;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record ReelSetNamed(
        String setName,
        List<List<Integer>> reelSet,
        @JsonInclude(JsonInclude.Include.NON_NULL) Double chance,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<List<Double>> reelTileChances
) {}
