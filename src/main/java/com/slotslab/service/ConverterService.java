package com.slotslab.service;

import com.slotslab.api.ConvertRequest;
import com.slotslab.convert.CsvConverter;
import com.slotslab.convert.CountConverter;
import com.slotslab.convert.JsonArrayConverter;
import com.slotslab.reel.Output;
import com.slotslab.reel.ReelSetsCollectionData;
import com.slotslab.reel.Strategy;
import com.slotslab.wrapper.ConvertWrapper;
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
