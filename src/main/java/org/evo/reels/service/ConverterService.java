package org.evo.reels.service;

import org.evo.reels.api.ConvertRequest;
import org.evo.reels.convert.CayConverter;
import org.evo.reels.convert.CountConverter;
import org.evo.reels.convert.EvoConverter;
import org.evo.reels.reel.Output;
import org.evo.reels.reel.ReelSetsCollectionData;
import org.evo.reels.reel.Strategy;
import org.evo.reels.wrapper.ConvertWrapper;
import org.springframework.stereotype.Service;

@Service
public class ConverterService {

    public String convert(ConvertRequest req) {
        return switch (req.toCom()) {
            case COUNT      -> CountConverter.convert(req.reelSets());
            case JSON_ARRAY -> EvoConverter.convert(req.reelSets());
            case CSV        -> {
                var config = new ReelSetsCollectionData(
                        null, req.gameId(),
                        Strategy.SHUFFLE, Output.stdout, null,
                        new ConvertWrapper(false, null, null, null),
                        null);
                yield CayConverter.convert(config, req.reelSets());
            }
            default -> throw new IllegalArgumentException("Unknown converter: " + req.toCom());
        };
    }
}
