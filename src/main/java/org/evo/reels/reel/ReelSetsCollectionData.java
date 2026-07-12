package org.evo.reels.reel;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.evo.reels.wrapper.ConvertWrapper;

import java.util.List;

public record ReelSetsCollectionData(
        @JsonProperty("mapName")        @JsonAlias({"gameName", "gameAbb", "abb", "abbreviation"}) String mapName,
        @JsonProperty("gameId")         @JsonAlias({"id"})      String gameId,
        @JsonProperty("strategy")       @JsonAlias({"type"})    Strategy strategy,
        @JsonProperty("output")         @JsonAlias({"print", "result", "res"}) Output output,
        @JsonProperty("resultFilePath") @JsonAlias({"tar", "target"}) String resultFilePath,
        @JsonProperty("convert")        @JsonAlias({"convertWrapper"}) ConvertWrapper convert,
        @JsonProperty("reelSets")       @JsonAlias({"reels", "sets"}) List<ReelSet> reelSets
) {}
