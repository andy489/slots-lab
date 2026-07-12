package com.slotslab.reels.api;

import com.slotslab.reels.convert.ConverterType;
import com.slotslab.reels.reel.ReelSetNamed;

import java.util.List;

public record ConvertRequest(
        List<ReelSetNamed> reelSets,
        ConverterType toCom,
        String gameId
) {}
