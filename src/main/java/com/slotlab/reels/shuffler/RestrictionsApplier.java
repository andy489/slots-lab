package com.slotlab.reels.shuffler;

import com.slotlab.reels.reel.Restriction;
import com.slotlab.reels.rng.RNG;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class RestrictionsApplier {
    private static final RNG rng = new RNG();

    public static LinkedList<Integer> get(final Map<Integer, Integer> tilesCnt, final Restriction restriction) {
        List<Integer> stackSizes  = restriction.stacks();
        List<Double>  stackChances = restriction.chances();
        int           distance    = restriction.distance();

        LinkedList<Integer> res = new LinkedList<>();
        Map<Integer, Integer> forbiddenByDist = new TreeMap<>();

        int currTriesBeforeTraversing = 0;
        int currTraversalTries = 0;

        while (!tilesCnt.isEmpty()) {
            int nth = rng.getRandInRange(0, tilesCnt.size());
            int tileToPlace = tilesCnt.keySet().stream()
                    .skip(nth)
                    .findFirst()
                    .orElseThrow();

            int currCntTile = tilesCnt.get(tileToPlace);
            if (currCntTile == 0) {
                tilesCnt.remove(tileToPlace);
                continue;
            }

            int currStackSz = rng.getWeightedRand(stackSizes, stackChances);
            int repetitions = Math.min(currStackSz, currCntTile);

            if (forbiddenByDist.getOrDefault(tileToPlace, Integer.MAX_VALUE) < distance) {
                currTriesBeforeTraversing++;

                if (currTriesBeforeTraversing > tilesCnt.size() * 3) {
                    currTraversalTries++;
                    boolean placed = placeStackedTilesWithoutConflicts(
                            res, tileToPlace, Collections.nCopies(repetitions, tileToPlace), distance);

                    if (placed) {
                        updateTilesCount(repetitions, currStackSz, tilesCnt, tileToPlace);
                        currTriesBeforeTraversing = 0;
                        currTraversalTries = 0;
                    } else if (currTraversalTries > 3) {
                        return null;
                    }
                }
            } else {
                updateTilesCount(repetitions, currStackSz, tilesCnt, tileToPlace);
                res.addAll(Collections.nCopies(repetitions, tileToPlace));
                actualizeForbiddenSet(forbiddenByDist, repetitions, tileToPlace);
                currTriesBeforeTraversing = 0;
            }
        }

        int tryCnt = 0;
        while (!validateNoHeadTailConflicts(res, distance)) {
            tryToFixHeadTailConflicts(res, distance);
            if (++tryCnt > 1_000) return null;
        }
        return res;
    }

    public static boolean placeStackedTilesWithoutConflicts(
            LinkedList<Integer> res,
            int tileToPlace,
            Collection<Integer> toPlace,
            int dist
    ) {
        int size = res.size() - 2 * dist;
        for (int i = 0; i < size; i++) {
            boolean canPlace = true;
            int k = 0;
            for (; k < dist; k++) {
                if (res.get(i + k) == tileToPlace || res.get(i + dist + k) == tileToPlace) {
                    canPlace = false;
                    break;
                }
            }
            if (canPlace && !Objects.equals(res.get(i + k - 1), res.get(i + k))) {
                res.addAll(i + k, toPlace);
                return true;
            }
        }
        return false;
    }

    public static void tryToFixHeadTailConflicts(LinkedList<Integer> res, int dist) {
        if (res.isEmpty()) return;

        Collection<Integer> toPlace = new LinkedList<>();
        int last = res.getLast();
        for (int i = res.size() - 1; i >= 0 && res.get(i) == last; i--) {
            toPlace.add(res.pollLast());
        }
        placeStackedTilesWithoutConflicts(res, last, toPlace, dist);
    }

    public static boolean validateNoHeadTailConflicts(LinkedList<Integer> res, int dist) {
        if (res.isEmpty()) return false;
        int size = res.size();
        for (int i = 0; i < dist; i++) {
            for (int j = 0; j < dist - i; j++) {
                if (Objects.equals(res.get(i), res.get(size - 1 - j))) return false;
            }
        }
        return true;
    }

    private static void actualizeForbiddenSet(Map<Integer, Integer> forbidden, int repetitions, int tile) {
        forbidden.replaceAll((k, v) -> v + repetitions);
        forbidden.put(tile, 0);
    }

    private static void updateTilesCount(int repetitions, int stackSz, Map<Integer, Integer> tilesCnt, int tile) {
        if (repetitions < stackSz) {
            tilesCnt.remove(tile);
        } else {
            tilesCnt.put(tile, tilesCnt.get(tile) - stackSz);
        }
    }
}
