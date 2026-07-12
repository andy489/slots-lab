package org.evo.reels.shuffler;

import org.evo.reels.reel.ReelSet;
import org.evo.reels.reel.ReelSetsCollectionData;

import java.util.List;
import java.util.StringJoiner;

public class FlatGenerator {

    public static String generateFlatReels(ReelSetsCollectionData collection) {
        List<ReelSet> reelSets = collection.reelSets();
        var sb = new StringBuilder("\n[\n");

        for (int i = 0; i < reelSets.size(); i++) {
            var reelJoiner = new StringJoiner(",\n\t\t\t", "\t\t\t", "");
            for (List<Integer> counts : reelSets.get(i).tilesCounts()) {
                reelJoiner.add(flatReel(counts));
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
