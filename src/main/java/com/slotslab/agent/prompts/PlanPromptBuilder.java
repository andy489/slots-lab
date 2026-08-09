package com.slotslab.agent.prompts;

import com.slotslab.agent.model.AgentRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanPromptBuilder {

    private final String skillDocs;

    public PlanPromptBuilder(String skillDocs) {
        this.skillDocs = skillDocs;
    }

    public String build(AgentRequest request, String paramsJson,
                        int winSymCount, int suggestedIter) {
        return """
            You are a slot game math expert. Your job is to choose the best initial generation
            parameters for an LTR (left-to-right) reel pipeline based on the targets below.

            ## Target objectives
            - targetRtp:       %s%%
            - rtpDelta:        ±%s%%
            - targetHitRate:   %s%%
            - hitRateDelta:    ±%s%%
            - targetVolatility: %s
            - maxPayout:       %s  (0 = uncapped; otherwise no single multiplier may exceed this value)

            ## User parameters (JSON — copy "symbols" and "lines" exactly if present)
            %s

            ## Skill documentation
            %s

            ## Bisection rule for maxIterations (MUST follow)
            Non-special symbol count (junior + senior) detected: %d
            Suggested maxIterations: %d  (= symbolCount × 20, clamped 80–200)
            Use this value — do NOT go lower.

            ## RTP math
            RTP ≈ Σ(weight[win_set_i] × avg_payout_i)
            More win sets → each starts with a smaller share → more iterations needed.
            For targetRtp > 90%%, use symsPerReel >= 128.

            ## Paytable design rules
            - Symbols are tiered: junior = common, low pay; senior = rare, high pay.
            - The paytable must be keyed by symbolId (integer as string) → { matchCount: multiplier }.
            - matchCount values must cover minMatch through screenWidth (e.g. 3,4,5 for minMatch=3, width=5).
            - Senior multipliers MUST be larger than junior multipliers at every matchCount.
            - For targetRtp %s%% with volatility %s, anchor 3-of-a-kind (minMatch) multipliers roughly:
                Junior  (tier="junior"): 0.3×–2.0× the bet per line
                Senior  (tier="senior"): 3×–10× the bet per line (higher for EXTREME/ULTRA_EXTREME)
            - Multiply by ~3× per extra reel for CASUAL, ~4–5× for HIGH/VERY_HIGH.
            - If maxPayout > 0, cap every multiplier at maxPayout.

            ## Initial weights design rules
            The weights array has length = noWinSetCount + winSetCount.
            noWinSetCount = minMatch (fixed — see spiral no-win factory).
            winSetCount   = number of junior + senior symbols.
            - No-win share ≈ clamp(1 - targetRtp/100, 0.03, 0.55). Split equally across no-win sets.
            - Win share = 1 − no-win share. Distribute across win sets: junior sets get more weight
              (they pay less), senior sets get less weight (they pay more). Use inverse rank scaling.
            - All weights > 0, sum = 1.0.

            ## Your task
            Output ONLY a valid JSON object (no markdown, no explanation) with ALL of these keys:
            {
              "strategy":        "LTR",
              "screenWidth":     <int, default 5>,
              "screenHeight":    <int, default 3>,
              "minMatch":        <int, default 3>,
              "symsPerReel":     <int, 64–512>,
              "maxIterations":   <int, prescribed above>,
              "spinsPerIter":    <long, default 1000000>,
              "targetVolatility":"<LOW|CASUAL|HIGH|VERY_HIGH|EXTREME|ULTRA_EXTREME>",
              "winVecDecay":     <double, 0.40–0.95, null = use volatility default>,
              "symbols":         [{"symbolId":<int>,"tier":"<junior|senior|wild|scatter|multiwild>","hint":"<string>"}],
              "lines":           [[<row indices per reel>], ...],
              "paytable":        {"<symbolId>": {"<matchCount>": <multiplier>, ...}, ...},
              "weights":         [<double>, ...]
            }
            The number of no-win reel sets is fixed at minMatch (the distinct spiral break phases) — it is not configurable.
            "winVecDecay" overrides the volatility-derived decay factor for win reel strips.
            "paytable" and "weights" are your initial guess — they will be refined by the iterate loop.
            """.formatted(
                request.targetRtp(), request.rtpDelta(),
                request.targetHitRate(), request.hitRateDelta(),
                request.targetVolatility(),
                request.maxPayout() > 0 ? request.maxPayout() + "×" : "uncapped",
                paramsJson,
                skillDocs,
                winSymCount,
                suggestedIter,
                request.targetRtp(), request.targetVolatility());
    }
}
