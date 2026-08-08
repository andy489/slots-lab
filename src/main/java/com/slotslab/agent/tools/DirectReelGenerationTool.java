package com.slotslab.agent.tools;

import com.slotslab.agent.model.GeneratedReels;
import com.slotslab.reel.ReelSetsCollectionData;
import com.slotslab.service.GeneratorService;
import org.springframework.stereotype.Component;

@Component
public class DirectReelGenerationTool implements ReelGenerationTool {

    private final GeneratorService generatorService;

    public DirectReelGenerationTool(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @Override
    public GeneratedReels generate(ReelSetsCollectionData config) {
        String rawJson = generatorService.generate(config);
        return new GeneratedReels(config, rawJson);
    }
}
