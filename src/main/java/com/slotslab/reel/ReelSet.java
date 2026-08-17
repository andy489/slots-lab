package com.slotslab.reel;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReelSet(
        @JsonProperty("reelSetName") @JsonAlias({"setName", "name"}) String reelSetName,
        @JsonProperty("tilesCounts") @JsonAlias({"counts", "cnt", "cnts", "tiles", "reelSetTileCounts"}) List<List<Integer>> tilesCounts,
        @JsonProperty("restrictions") @JsonAlias({"res"}) List<Restriction> restrictions
) {}
