package com.slotlab.reels.service;

import com.slotlab.reels.api.ConvertRequest;
import com.slotlab.reels.convert.CsvConverter;
import com.slotlab.reels.convert.CountConverter;
import com.slotlab.reels.convert.JsonArrayConverter;
import com.slotlab.reels.reel.Output;
import com.slotlab.reels.reel.ReelSetsCollectionData;
import com.slotlab.reels.reel.Strategy;
import com.slotlab.reels.wrapper.ConvertWrapper;
import org.springframework.stereotype.Service;

@Service
public class ConverterService {

    public String convert(ConvertRequest req) {
        return switch (req.toCom()) {
            case COUNT      -> CountConverter.convert(req.reelSets());
            case JSON_ARRAY -> JsonArrayConverter.convert(req.reelSets());
            case CSV        -> {
                var config = new ReelSetsCollectionData(
                        null, req.gameId(),
                        Strategy.SHUFFLE, Output.stdout, null,
                        new ConvertWrapper(false, null, null, null),
                        null);
                yield CsvConverter.convert(config, req.reelSets());
            }
            default -> throw new IllegalArgumentException("Unknown converter: " + req.toCom());
        };
    }
}
