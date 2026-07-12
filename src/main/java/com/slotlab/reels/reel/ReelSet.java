package com.slotlab.reels.reel;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReelSet(
        @JsonProperty("tilesCounts") @JsonAlias({"counts", "cnt", "cnts", "tiles", "reelSetTileCounts"}) List<List<Integer>> tilesCounts,
        @JsonProperty("restrictions") @JsonAlias({"res"}) List<Restriction> restrictions
) {}
