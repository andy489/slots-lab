package com.slotslab.convert;

import com.slotslab.reel.ReelSetNamed;

import java.util.List;
import java.util.StringJoiner;

public class CsvConverter {
    public static String convert(String gameId, List<ReelSetNamed> reelSets) {
        var sb = new StringBuilder();

        for (ReelSetNamed rs : reelSets) {
            List<List<Integer>> reelSet = rs.reelSet();
            for (int j = 0; j < reelSet.size(); j++) {
                List<Integer> reel = reelSet.get(j);
                for (int k = 0; k < reel.size(); k++) {
                    int tile = reel.get(k);
                    sb.append('"').append(rs.setName()).append("\",")
                      .append('"').append(j).append("\",")
                      .append('"').append(k).append("\",")
                      .append('"').append(gameId)
                      .append(String.format("%03d", tile))
                      .append('"').append('\n');
                }
            }
        }

        return sb.toString();
    }
}
