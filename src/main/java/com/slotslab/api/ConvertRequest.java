package com.slotslab.api;

import com.slotslab.convert.ConverterType;
import com.slotslab.reel.ReelSetNamed;

import java.util.List;

public record ConvertRequest(
        List<ReelSetNamed> reelSets,
        ConverterType toCom,
        String gameId
) {}
