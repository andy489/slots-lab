package com.slotslab.agent.model;

import com.slotslab.reel.ReelSetsCollectionData;

public record GeneratedReels(
        ReelSetsCollectionData config,
        String rawJson
) {}
