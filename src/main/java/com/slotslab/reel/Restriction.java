package com.slotslab.reel;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Restriction(
        @JsonProperty("stackSizes")   @JsonAlias({"stack", "stackSizes"})         List<Integer> stacks,
        @JsonProperty("stackChances") @JsonAlias({"chance", "stackChances", "stacksChances"}) List<Double> chances,
        @JsonProperty("minDistance")  @JsonAlias({"dist", "minDistance", "minStackDistance"}) int distance
) {}
