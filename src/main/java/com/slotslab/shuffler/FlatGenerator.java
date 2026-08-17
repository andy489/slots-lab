package com.slotslab.shuffler;

import com.slotslab.reel.ReelSet;
import com.slotslab.reel.ReelSetsCollectionData;

import java.util.List;
import java.util.StringJoiner;

public class FlatGenerator {

    public static String generateFlatReels(ReelSetsCollectionData collection) {
        List<ReelSet> reelSets = collection.reelSets();
        var sb = new StringBuilder("\n[\n");

        for (int i = 0; i < reelSets.size(); i++) {
            ReelSet reelSet = reelSets.get(i);
            var reelJoiner = new StringJoiner(",\n\t\t\t", "\t\t\t", "");
            for (List<Integer> counts : reelSet.tilesCounts()) {
                reelJoiner.add(flatReel(counts));
            }
            sb.append("\t{\n")
              .append("\t\t\"setName\": \"").append(reelSet.reelSetName() != null ? reelSet.reelSetName() : "ReelSet#" + i).append("\",\n")
              .append("\t\t\"reelSet\": [\n")
              .append(reelJoiner)
              .append("\n\t\t]\n\t}");

            if (i < reelSets.size() - 1) sb.append(",");
            sb.append("\n");
        }

        return sb.append("]").toString();
    }

    private static String flatReel(List<Integer> counts) {
        var sj = new StringJoiner(",", "[", "]");
        for (int tileId = 0; tileId < counts.size(); tileId++) {
            int count = Math.max(0, counts.get(tileId));
            for (int j = 0; j < count; j++) {
                sj.add(String.valueOf(tileId + 1));
            }
        }
        return sj.toString();
    }
}
