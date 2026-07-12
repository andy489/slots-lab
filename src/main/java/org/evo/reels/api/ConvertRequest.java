package org.evo.reels.api;

import org.evo.reels.convert.ConverterType;
import org.evo.reels.reel.ReelSetEvo;

import java.util.List;

public record ConvertRequest(
        List<ReelSetEvo> reelSets,
        ConverterType toCom,
        String gameId
) {}
