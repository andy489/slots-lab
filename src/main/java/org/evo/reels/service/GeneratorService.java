package org.evo.reels.service;

import org.evo.reels.reel.ReelSetsCollectionData;
import org.evo.reels.shuffler.FlatGenerator;
import org.evo.reels.shuffler.ShuffleGenerator;
import org.springframework.stereotype.Service;

@Service
public class GeneratorService {

    private final ConfigValidator validator;

    public GeneratorService(ConfigValidator validator) {
        this.validator = validator;
    }

    public String generate(ReelSetsCollectionData config) {
        validator.validate(config);
        return switch (config.strategy()) {
            case FLAT    -> FlatGenerator.generateFlatReels(config);
            case SHUFFLE -> ShuffleGenerator.generateStackedReels(config);
            default      -> throw new IllegalArgumentException("Unknown strategy: " + config.strategy());
        };
    }
}
