package org.evo.reels.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

@Service
public class HistoryService {

    private static final Path GENERATE_DIR = Paths.get("history", "generate");
    private static final Path SIMULATE_DIR = Paths.get("history", "simulate");
    private final ObjectMapper mapper = new ObjectMapper();

    public HistoryService() throws IOException {
        Files.createDirectories(GENERATE_DIR);
        Files.createDirectories(SIMULATE_DIR);
    }

    public List<HistoryEntry> list(String kind) throws IOException {
        Path dir = dir(kind);
        if (!Files.exists(dir)) return List.of();
        try (var stream = Files.list(dir)) {
            return stream
                .filter(p -> p.toString().endsWith(".json"))
                .sorted(Comparator.reverseOrder())
                .map(p -> {
                    try { return mapper.readValue(p.toFile(), HistoryEntry.class); }
                    catch (IOException e) { return null; }
                })
                .filter(e -> e != null)
                .toList();
        }
    }

    public HistoryEntry save(String kind, HistoryEntry entry) throws IOException {
        Path file = dir(kind).resolve(entry.id() + ".json");
        mapper.writeValue(file.toFile(), entry);
        return entry;
    }

    public void resize(String kind, int max) throws IOException {
        List<HistoryEntry> entries = list(kind);
        if (entries.size() > max) {
            List<HistoryEntry> toDelete = entries.subList(max, entries.size());
            for (HistoryEntry e : toDelete) {
                Files.deleteIfExists(dir(kind).resolve(e.id() + ".json"));
            }
        }
    }

    public void clearAll(String kind) throws IOException {
        Path dir = dir(kind);
        if (!Files.exists(dir)) return;
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                  .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }

    private Path dir(String kind) {
        return "simulate".equals(kind) ? SIMULATE_DIR : GENERATE_DIR;
    }
}
