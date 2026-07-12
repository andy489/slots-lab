package org.evo.reels.convert;

import org.evo.reels.reel.ReelSetEvo;

import java.util.List;
import java.util.StringJoiner;

public class EvoConverter {
    public static String convert(List<ReelSetEvo> reelSets) {
        var outer = new StringJoiner(",\n", "[\n", "\n]");
        for (ReelSetEvo rs : reelSets) {
            var reelJoiner = new StringJoiner(",\n\t\t\t", "\t\t\t", "");
            for (List<Integer> reel : rs.reelSet()) {
                reelJoiner.add(formatReel(reel));
            }
            outer.add("\t{\n\t\t\"setName\": \"" + rs.setName() + "\",\n\t\t\"reelSet\": [\n"
                    + reelJoiner + "\n\t\t]\n\t}");
        }
        return outer.toString();
    }

    private static String formatReel(List<Integer> reel) {
        var sb = new StringBuilder("[");
        for (int i = 0; i < reel.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(reel.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
