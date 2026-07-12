package com.slotslab.reels.convert;

import com.slotslab.reels.reel.ReelSetNamed;

import java.util.List;
import java.util.StringJoiner;

public class JsonArrayConverter {
    public static String convert(List<ReelSetNamed> reelSets) {
        var outer = new StringJoiner(",\n", "[\n", "\n]");
        for (ReelSetNamed rs : reelSets) {
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
