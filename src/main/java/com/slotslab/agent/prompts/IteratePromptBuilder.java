package com.slotslab.agent.prompts;

import com.slotslab.agent.model.AgentContext;
import com.slotslab.agent.model.AgentRequest;

import java.util.List;
import java.util.stream.Collectors;

public class IteratePromptBuilder {

    private final String skillDocs;

    public IteratePromptBuilder(String skillDocs) {
        this.skillDocs = skillDocs;
    }

    public String build(AgentRequest request,
                        int iteration,
                        String simJson,
                        List<String> violations,
                        String stateJson,
                        int nextIterHint,
                        int nextSeed,
                        double actualRtp,
                        double gapRatio,
                        List<AgentContext.LlmCallRecord> history) {

        double rtpGap = actualRtp - request.targetRtp();
        boolean rtpTooLow  = rtpGap < -request.rtpDelta();
        boolean rtpTooHigh = rtpGap >  request.rtpDelta();

        String gapLabel;
        String urgentDiagnosis;

        if (gapRatio > 0.20) {
            gapLabel = "LARGE (>20%% of target)";
            if (rtpTooLow) {
                urgentDiagnosis = """
                    ⚠️  CRITICAL: RTP IS %.2f%% BELOW TARGET (%.2f%% vs %.2f%%).
                    Shuffling weights ALONE cannot bridge a %.1f%% gap — you MUST act on paytable AND weights:
                    1. RAISE all senior symbol multipliers by 2×–5× (e.g. if 3x=2.0 → set to 4.0–10.0).
                    2. RAISE junior 5-of-a-kind multipliers to be at least 1× the bet.
                    3. SHIFT weights heavily toward win reel sets (win weights ≥ 0.60 of total).
                    4. If still stuck after 2 iterations: lower winVecDecay to 0.50.
                    DO NOT output only a weights patch — that is INSUFFICIENT for this gap size.
                    """.formatted(Math.abs(rtpGap), actualRtp, request.targetRtp(), Math.abs(rtpGap));
            } else {
                urgentDiagnosis = """
                    ⚠️  RTP IS %.2f%% ABOVE TARGET (%.2f%% vs %.2f%%).
                    1. CUT all senior multipliers by 50%%.
                    2. SHIFT weights toward no-win reel sets (no-win weights ≥ 0.60 of total).
                    3. If still stuck: raise winVecDecay toward 0.90 to thin out win strips.
                    """.formatted(Math.abs(rtpGap), actualRtp, request.targetRtp());
            }
        } else if (gapRatio > 0.05) {
            gapLabel = "MEDIUM (5–20%% of target)";
            if (rtpTooLow) {
                urgentDiagnosis = """
                    RTP is %.2f%% below target. Raise paytable multipliers for senior symbols by ~1.5× \
                    AND shift weights toward win sets by 20%%.
                    """.formatted(Math.abs(rtpGap));
            } else {
                urgentDiagnosis = """
                    RTP is %.2f%% above target. Lower senior multipliers slightly AND shift weights toward no-win sets.
                    """.formatted(Math.abs(rtpGap));
            }
        } else {
            gapLabel = "SMALL (≤5%% of target) — fine-tune weights and/or minor paytable tweaks";
            urgentDiagnosis = "Gap is small — minor weight redistribution or paytable microadjustment is sufficient.";
        }

        String historySection = buildHistorySection(history);

        return """
            You are a slot game math expert analysing iteration %d of a reel generation run.
            Study the skill docs, original targets, current state, simulation output, and PRIOR ITERATION HISTORY.
            Output a JSON patch — only the keys that need to change.

            ## Original targets (fixed — never output these as patch keys)
            - targetRtp:        %.2f%%  (±%.2f%%)
            - targetHitRate:    %.2f%%  (±%.2f%%)
            - targetVolatility: %s
            - maxPayout:        %s  (hard cap — no multiplier may exceed this)

            ## ⚠️  CURRENT GAP DIAGNOSIS — READ CAREFULLY BEFORE PATCHING
            - actualRtp detected : %.2f%%
            - targetRtp          : %.2f%%
            - absolute gap       : %.2f%% → %s
            %s

            ## Current mutable state
            %s

            ## Violations detected
            %s

            ## Last simulation output
            %s

            ## Prior iteration history (most recent first — learn from what did NOT work)
            %s

            ## Skill documentation
            %s

            ## Iteration budget (fixed by the user — you CANNOT change these)
            - maxIterations: %d   (hard cap on total iterations — set in the UI, not patchable)
            - seed:          %d

            ## Patchable keys (output ONLY the keys you want to change)
            | Key             | Type            | Effect |
            |-----------------|-----------------|--------|
            | weights         | array<double>   | Probability of each reel set. Must sum to ~1.0. Length must match current weights array. |
            | paytable        | object          | symbolId → { matchCount: multiplier }. Patch individual symbols. Sub-1x junior multipliers (0.1–0.9) are valid — they create frequent cheap wins without inflating RTP. |
            | winVecDecay     | double (0.4–0.95)| Win strip tile decay per reel. Change triggers strip rebuild. |
            | symsPerReel     | int (64–512)    | Total tiles per reel. Change triggers strip rebuild. |
            | targetVolatility| string          | Volatility level. Change triggers full rebuild (paytable + strips). |

            ## Mandatory rules
            1. For LARGE gap (>20%% of target): you MUST patch BOTH paytable AND weights. A weights-only patch is FORBIDDEN.
            2. For MEDIUM gap (5–20%%): patch paytable AND weights together for fastest convergence.
            3. For SMALL gap (≤5%%): weights alone or minor paytable tweak is acceptable.
            4. Never output targetRtp, rtpDelta, targetHitRate, hitRateDelta, symbols, lines, maxIterations, or seed.
            5. Weights patch: recalculate the full array so all values > 0 and sum = 1.0.
            6. If maxPayout > 0: never patch any multiplier above maxPayout.
            7. Senior multipliers must always exceed junior multipliers at every match length.
            8. Do NOT repeat a patch that failed in a prior iteration — look at history and try a DIFFERENT approach.

            Output ONLY a valid JSON object. No markdown, no explanation.
            """.formatted(
                iteration,
                request.targetRtp(), request.rtpDelta(),
                request.targetHitRate(), request.hitRateDelta(),
                request.targetVolatility(),
                request.maxPayout() > 0 ? request.maxPayout() + "×" : "uncapped",
                actualRtp, request.targetRtp(),
                Math.abs(rtpGap), gapLabel, urgentDiagnosis,
                stateJson,
                violations.isEmpty() ? "none" : String.join("\n", violations),
                simJson == null ? "unavailable" : truncate(simJson, 3000),
                historySection,
                skillDocs,
                nextIterHint, nextSeed);
    }

    private String buildHistorySection(List<AgentContext.LlmCallRecord> history) {
        if (history == null || history.isEmpty()) return "No prior iterations.";
        // Include last 3 iterations max to avoid huge prompts
        int start = Math.max(0, history.size() - 3);
        List<AgentContext.LlmCallRecord> recent = history.subList(start, history.size());
        StringBuilder sb = new StringBuilder();
        for (int i = recent.size() - 1; i >= 0; i--) {
            AgentContext.LlmCallRecord rec = recent.get(i);
            sb.append("### Iteration ").append(rec.iteration()).append(" response (what LLM patched):\n");
            sb.append(truncate(rec.response(), 800)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "... [truncated]";
    }
}
