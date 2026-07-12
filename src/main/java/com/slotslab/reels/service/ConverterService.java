package com.slotslab.reels.service;

import com.slotslab.reels.api.ConvertRequest;
import com.slotslab.reels.convert.CsvConverter;
import com.slotslab.reels.convert.CountConverter;
import com.slotslab.reels.convert.JsonArrayConverter;
import com.slotslab.reels.reel.Output;
import com.slotslab.reels.reel.ReelSetsCollectionData;
import com.slotslab.reels.reel.Strategy;
import com.slotslab.reels.wrapper.ConvertWrapper;
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
