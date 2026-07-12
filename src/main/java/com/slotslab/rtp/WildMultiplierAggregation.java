package com.slotslab.rtp;

/**
 * How multiple wild multipliers in a single winning line are combined.
 * Extensible: add new aggregation types here; RtpWorker picks them up automatically
 * via WildMultiplierAggregator.
 */
public enum WildMultiplierAggregation {
    /** Wild acts as a pure substitute — no multiplier applied (lineMultiplier = 1.0). */
    NONE,
    /** Sum all participating wild multipliers. */
    ADD,
    /** Multiply all participating wild multipliers together. */
    MULTIPLY,
    /** Look up the combined multiplier from a fixed sequence indexed by wild count on the line. */
    SEQUENCE
}
