package com.slotlab.reels.shuffler;

import com.slotlab.reels.reel.ReelSet;
import com.slotlab.reels.reel.ReelSetsCollectionData;
import com.slotlab.reels.reel.Restriction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

public class ShuffleGenerator {
    private static final int RETRIES = 8;

    public static String generateStackedReels(ReelSetsCollectionData collection) {
        var sb = new StringBuilder("\n[\n");

        List<ReelSet> reelSets = collection.reelSets();
        for (int i = 0; i < reelSets.size(); i++) {
            ReelSet reelSet = reelSets.get(i);
            List<Restriction> restrictions = reelSet.restrictions() != null ? reelSet.restrictions() : List.of();
            int numReels = reelSet.tilesCounts().size();
            int numRestrictions = restrictions.size();

            var reelJoiner = new StringJoiner(",\n\t\t\t", "\t\t\t", "");

            for (int j = 0; j < numReels; j++) {
                Map<Integer, Integer> tilesCnt = buildTilesMap(reelSet.tilesCounts().get(j));
                if (numRestrictions == 0) {
                    reelJoiner.add(randomShuffleReelToString(tilesCnt));
                    continue;
                }
                Restriction restriction = restrictions.get(j % numRestrictions);

                LinkedList<Integer> reel = tryGenerate(tilesCnt, restriction);
                reelJoiner.add(reel != null ? reelToString(reel) : "[]");
            }

            sb.append("\t{\n")
              .append("\t\t\"setName\": \"ReelSet#").append(i).append("\",\n")
              .append("\t\t\"reelSet\": [\n")
              .append(reelJoiner)
              .append("\n\t\t]\n\t}");

            if (i < reelSets.size() - 1) sb.append(",");
            sb.append("\n");
        }

        return sb.append("]").toString();
    }

    private static LinkedList<Integer> tryGenerate(Map<Integer, Integer> tilesCnt, Restriction restriction) {
        for (int attempt = 0; attempt < RETRIES; attempt++) {
            Map<Integer, Integer> copy = new TreeMap<>(tilesCnt);
            LinkedList<Integer> result = RestrictionsApplier.get(copy, restriction);
            if (result != null) return result;
        }
        return null;
    }

    private static String reelToString(LinkedList<Integer> reel) {
        var sj = new StringJoiner(",", "[", "]");
        reel.forEach(t -> sj.add(String.valueOf(t)));
        return sj.toString();
    }

    private static String randomShuffleReelToString(Map<Integer, Integer> tilesCnt) {
        var tiles = new ArrayList<Integer>();
        tilesCnt.forEach((tile, count) -> {
            for (int i = 0; i < count; i++) tiles.add(tile);
        });
        Collections.shuffle(tiles);
        var sj = new StringJoiner(",", "[", "]");
        tiles.forEach(t -> sj.add(String.valueOf(t)));
        return sj.toString();
    }

    private static String flatReelToString(Map<Integer, Integer> tilesCnt) {
        var sj = new StringJoiner(",", "[", "]");
        tilesCnt.forEach((tile, count) -> {
            for (int i = 0; i < count; i++) sj.add(String.valueOf(tile));
        });
        return sj.toString();
    }

    private static Map<Integer, Integer> buildTilesMap(List<Integer> counts) {
        Map<Integer, Integer> map = new TreeMap<>();
        for (int i = 0; i < counts.size(); i++) {
            map.put(i + 1, counts.get(i));
        }
        return map;
    }
}
