package org.evo.reels.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evo.reels.exception.InvalidJsonFormatException;
import org.evo.reels.reel.ReelSetEvo;
import org.evo.reels.reel.ReelSetsCollectionData;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReaderManager {

    public static ReelSetsCollectionData readConfig(String filePath, ObjectMapper om) throws IOException {
        try {
            return om.readValue(Path.of(filePath).toFile(), ReelSetsCollectionData.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new InvalidJsonFormatException("Invalid JSON in " + filePath, e);
        }
    }

    public static List<ReelSetEvo> readEvoFile(String filePath, ObjectMapper om) throws IOException {
        try {
            return om.readValue(Path.of(filePath).toFile(), new TypeReference<>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            // not JSON — try CAY CSV format
        }
        return readCayCsv(filePath);
    }

    private static List<ReelSetEvo> readCayCsv(String filePath) throws IOException {
        List<ReelSetEvo> result = new ArrayList<>();
        String currentSetName = null;
        List<List<Integer>> currentReelSet = null;

        try (BufferedReader br = Files.newBufferedReader(Path.of(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length != 4) break;

                String setName = tokens[0].replaceAll("\"", "");
                int reelIdx = Integer.parseInt(tokens[1].replaceAll("\"", ""));
                int tile    = Integer.parseInt(tokens[3].replaceAll("\"", "")) % 100;

                if (!setName.equals(currentSetName)) {
                    currentSetName = setName;
                    currentReelSet = new ArrayList<>();
                    for (int i = 0; i < 10; i++) currentReelSet.add(new ArrayList<>());
                    result.add(new ReelSetEvo(setName, currentReelSet));
                }
                currentReelSet.get(reelIdx).add(tile);
            }
        }

        // remove empty reels
        for (ReelSetEvo rs : result) {
            rs.reelSet().removeIf(List::isEmpty);
        }
        return result;
    }
}
