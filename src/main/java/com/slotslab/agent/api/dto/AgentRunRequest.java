package com.slotslab.agent.api.dto;

import com.slotslab.reel.ReelSetsCollectionData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

public record AgentRunRequest(

        @DecimalMin(value = "0.01", message = "targetRtp must be > 0")
        @DecimalMax(value = "100.0", message = "targetRtp must be ≤ 100")
        double targetRtp,

        @DecimalMin(value = "0.0", message = "rtpDelta must be ≥ 0")
        double rtpDelta,

        @DecimalMin(value = "0.01", message = "targetHitRate must be > 0")
        @DecimalMax(value = "100.0", message = "targetHitRate must be ≤ 100")
        double targetHitRate,

        @DecimalMin(value = "0.0", message = "hitRateDelta must be ≥ 0")
        double hitRateDelta,

        @NotBlank(message = "targetVolatility is required")
        @Pattern(regexp = "LOW|CASUAL|HIGH|VERY_HIGH|EXTREME|ULTRA_EXTREME",
                 message = "targetVolatility must be one of: LOW, CASUAL, HIGH, VERY_HIGH, EXTREME, ULTRA_EXTREME")
        String targetVolatility,

        @DecimalMin(value = "0.0", message = "maxPayout must be ≥ 0")
        double maxPayout,

        ReelSetsCollectionData reelConfig,

        @NotNull(message = "parameters map is required")
        Map<String, Object> parameters

) {}
