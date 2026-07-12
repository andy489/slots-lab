package com.slotlab.reels.service;

import com.slotlab.reels.reel.ReelSetsCollectionData;
import com.slotlab.reels.shuffler.FlatGenerator;
import com.slotlab.reels.shuffler.ShuffleGenerator;
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
