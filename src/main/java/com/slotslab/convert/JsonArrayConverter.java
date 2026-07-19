package com.slotslab.convert;

import com.slotslab.reel.ReelSetNamed;

import java.util.List;
import java.util.StringJoiner;

public class JsonArrayConverter {
    public static String convert(List<ReelSetNamed> reelSets) {
        var outer = new StringJoiner(",\n", "[\n", "\n]");
        for (ReelSetNamed rs : reelSets) {
            var reelJoiner = new StringJoiner(",\n\t\t\t", "\t\t\t", "");
            for (List<Integer> reel : rs.reelSet()) {
                reelJoiner.add(formatIntReel(reel));
            }
            var sb = new StringBuilder();
            sb.append("\t{\n\t\t\"setName\": \"").append(rs.setName()).append("\",\n");
            sb.append("\t\t\"reelSet\": [\n").append(reelJoiner).append("\n\t\t]");
            if (rs.chance() != null) {
                sb.append(",\n\t\t\"chance\": ").append(formatDouble(rs.chance()));
            }
            if (rs.reelTileChances() != null) {
                var rtcJoiner = new StringJoiner(",\n\t\t\t", "\t\t\t", "");
                for (List<Double> row : rs.reelTileChances()) rtcJoiner.add(formatDoubleReel(row));
                sb.append(",\n\t\t\"reelTileChances\": [\n").append(rtcJoiner).append("\n\t\t]");
            }
            sb.append("\n\t}");
            outer.add(sb.toString());
        }
        return outer.toString();
    }

    private static String formatIntReel(List<Integer> reel) {
        var sb = new StringBuilder("[");
        for (int i = 0; i < reel.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(reel.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String formatDoubleReel(List<Double> row) {
        var sb = new StringBuilder("[");
        for (int i = 0; i < row.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatDouble(row.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String formatDouble(double v) {
        return v == Math.floor(v) && !Double.isInfinite(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
