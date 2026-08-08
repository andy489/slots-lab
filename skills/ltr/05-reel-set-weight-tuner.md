# ReelSetWeightTuner

## Purpose
Assign probability weights to all reel sets (no-win + winning) and iteratively adjust
them until the simulated RTP and hit rate both land within their target tolerances.

## Inputs
| Field         | Type                             | Description                                      |
|---------------|----------------------------------|--------------------------------------------------|
| noWinReelSets | List<int[][]>                    | No-win reel sets from SpiralNoWinReelSetFactory  |
| winReelSets   | List<int[][]>                    | Winning reel sets from WinReelSetFactory         |
| paytable      | Map<Integer,Map<Integer,Double>> | Output of PaytableGenerator                      |
| targetRtp     | double                           | e.g. 95.50                                       |
| rtpDelta      | double                           | e.g. 0.15                                        |
| targetHitRate | double                           | e.g. 20.00                                       |
| hitRateDelta  | double                           | e.g. 2.00                                        |
| lines         | List<int[]>                      | Pay lines (for LTR evaluation)                   |
| minMatch      | int                              | Minimum match length                             |
| screenWidth   | int                              | Number of reels                                  |
| screenHeight  | int                              | Number of rows                                   |
| maxIterations | int                              | Safety cap (default 50)                          |
| spinsPerEval  | long                             | Spins per simulated evaluation (default 500_000) |

## Outputs
`WeightedReelSets` — all reel sets with a normalised double weight each, summing to 1.0.

## Algorithm
### Initial weights
- Each no-win reel set starts with weight = 1.0 / totalReelSets.
- Each winning reel set starts with weight = 1.0 / totalReelSets.
- Normalize so all weights sum to 1.0.

### Feedback loop (repeat up to maxIterations)
1. **Simulate** `spinsPerEval` spins using the current weights to sample a reel set,
   then randomly pick stops and evaluate LTR wins using the paytable and lines.
2. Compute `actualRtp` and `actualHitRate` from simulation.
3. **RTP control**:
   - If `actualRtp < targetRtp − rtpDelta`: increase total weight of all winning reel
     sets by factor 1.05; decrease no-win weight by the same total amount.
   - If `actualRtp > targetRtp + rtpDelta`: decrease winning reel sets weight by 1.05.
4. **Hit-rate control**:
   - If `actualHitRate < targetHitRate − hitRateDelta`: decrease no-win reel set weight
     proportionally (redistribute to winning sets).
   - If `actualHitRate > targetHitRate + hitRateDelta`: increase no-win reel set weight.
5. Re-normalize weights after each adjustment.
6. Stop when both RTP and hit rate are within tolerance, or maxIterations reached.

## Constraints
- No individual reel set weight may fall below 0.001 (prevents zeroing out a set entirely).
- Final weights are normalized to sum to exactly 1.0.
- If convergence is not reached within maxIterations, return the best weights found
  (closest to both targets) and log a warning.
