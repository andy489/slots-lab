package com.slotslab.agent.model;

import com.slotslab.reel.ReelSetsCollectionData;

import java.util.Map;

public record AgentRequest(
        double targetRtp,
        double rtpDelta,
        double targetHitRate,
        double hitRateDelta,
        String targetVolatility,
        ReelSetsCollectionData reelConfig,
        Map<String, Object> parameters
) {}
