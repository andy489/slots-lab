package com.slotlab.reels.api;

import com.slotlab.reels.convert.ConverterType;
import com.slotlab.reels.reel.ReelSetNamed;

import java.util.List;

public record ConvertRequest(
        List<ReelSetNamed> reelSets,
        ConverterType toCom,
        String gameId
) {}
