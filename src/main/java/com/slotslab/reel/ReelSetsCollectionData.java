package com.slotslab.reel;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReelSetsCollectionData(
        @JsonProperty("strategy")  @JsonAlias({"type"})          Strategy strategy,
        @JsonProperty("reelSets")  @JsonAlias({"reels", "sets"}) List<ReelSet> reelSets
) {}
