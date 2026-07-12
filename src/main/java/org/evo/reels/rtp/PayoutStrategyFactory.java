package org.evo.reels.rtp;

public final class PayoutStrategyFactory {

    private PayoutStrategyFactory() {}

    public static PayoutStrategy create(PayoutStrategyType type) {
        return switch (type) {
            case LTR -> new LtrPayoutStrategy();
            case RTL -> new RtlPayoutStrategy();
            case BW  -> new BwPayoutStrategy();
        };
    }
}
