package com.slotlab.reels.convert;

import com.slotlab.reels.reel.ReelSetNamed;
import com.slotlab.reels.reel.ReelSetsCollectionData;

import java.util.List;
import java.util.StringJoiner;

public class CsvConverter {
    public static String convert(ReelSetsCollectionData collection, List<ReelSetNamed> reelSets) {
        String gameId = collection.gameId();
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
