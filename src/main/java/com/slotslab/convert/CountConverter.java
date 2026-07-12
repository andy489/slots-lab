package com.slotslab.convert;

import com.slotslab.reel.ReelSetNamed;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;

public class CountConverter {
    public static String convert(List<ReelSetNamed> reelSets) {
        var outer = new StringJoiner(",\n", "[\n", "\n]");

        for (ReelSetNamed rs : reelSets) {
            var countMaps = new ArrayList<TreeMap<Integer, Integer>>(rs.reelSet().size());

            for (List<Integer> reel : rs.reelSet()) {
                var map = new TreeMap<Integer, Integer>();
                for (int tile : reel) map.merge(tile, 1, Integer::sum);
                countMaps.add(map);
            }

            String setName = rs.setName().replaceAll("^\"|\"$", "");
            var reelJoiner = new StringJoiner(",\n\t\t\t", "\t\t\t", "");

            for (TreeMap<Integer, Integer> map : countMaps) {
                var sj = new StringJoiner(", ", "[", "]");
                int reelMax = map.isEmpty() ? 0 : map.lastKey();
                for (int t = 1; t <= reelMax; t++) sj.add(String.valueOf(map.getOrDefault(t, 0)));
                reelJoiner.add(sj.toString());
            }

            outer.add("\t{\n\t\t\"setName\": \"" + setName + "\",\n\t\t\"reelSetTileCounts\": [\n"
                    + reelJoiner + "\n\t\t]\n\t}");
        }

        return outer.toString();
    }
}
