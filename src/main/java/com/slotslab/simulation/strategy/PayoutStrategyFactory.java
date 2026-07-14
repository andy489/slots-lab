package com.slotslab.simulation.strategy;

public final class PayoutStrategyFactory {

    private PayoutStrategyFactory() {}

    public static PayoutStrategy create(PayoutStrategyType type) {
        return switch (type) {
            case LTR      -> new LtrPayoutStrategy();
            case RTL      -> new RtlPayoutStrategy();
            case BW       -> new BwPayoutStrategy();
            case ADJ      -> new AdjPayoutStrategy();
            case WAYS     -> new WaysPayoutStrategy();
            case SCATTERS -> new ScattersPayoutStrategy();
            case CLUSTERS -> new ClustersPayoutStrategy();
        };
    }
}
