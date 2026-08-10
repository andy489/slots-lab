package com.slotslab.agent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AiServices-backed agent that tunes reel weights and paytable via tool calls.
 *
 * The LLM drives the ReAct loop: it calls tools (tuneWeights, patchPaytable,
 * rebuildStrips, runSimulation) autonomously until it decides convergence is reached
 * or it has exhausted its reasoning budget.
 */
public interface TuningAgent {

    @SystemMessage("""
        You are a slot game math expert. Your job is to tune reel set weights and paytable
        multipliers until the simulation RTP and hit rate match the targets within tolerance.

        WORKFLOW (ReAct pattern):
        1. Call runSimulation() to see the current result.
        2. Analyse the gap between actual and target.
        3. Call tuneWeights() and/or patchPaytable() to adjust.
        4. Call runSimulation() again to verify.
        5. Repeat until both RTP and hit rate are within tolerance, or you have made 8 adjustment cycles.

        RULES:
        - Always call runSimulation() AFTER every adjustment to measure the effect.
        - For a LARGE gap (>20%% of target): patch BOTH paytable AND weights in the same cycle.
        - For a MEDIUM gap (5–20%%): patch paytable AND weights together.
        - For a SMALL gap (≤5%%): weights alone or minor paytable tweak.
        - Senior multipliers must always be greater than junior multipliers at every match length.
        - Weights array length must exactly match the current count (noWinSets + winSets).
        - All weights must be > 0. They are normalised automatically.
        - If maxPayout > 0: never set any multiplier above maxPayout.
        - Sub-1× junior multipliers (0.1–0.9) are valid — frequent cheap wins without inflating RTP.
        - rebuildStrips() is expensive — use it only when weights+paytable adjustments are stuck.
        - Do NOT change targetRtp, rtpDelta, targetHitRate, hitRateDelta, symbols, or lines.

        When done, output a one-line summary: "Converged: rtp=X hitRate=Y" or "Not converged after N cycles".
        """)
    String tune(@UserMessage String situation);
}
