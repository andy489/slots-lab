package com.slotslab.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

@Service
public class HistoryService {

    private static final Path BASE_DIR = Paths.get("history");
    private final ObjectMapper mapper = new ObjectMapper();

    public HistoryService() throws IOException {
        Files.createDirectories(BASE_DIR);
    }

    public List<HistoryEntry> list(String kind, String sessionId) throws IOException {
        Path dir = dir(kind, sessionId);
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

    public HistoryEntry save(String kind, String sessionId, HistoryEntry entry) throws IOException {
        Path dir = dir(kind, sessionId);
        Files.createDirectories(dir);
        Path file = dir.resolve(entry.id() + ".json");
        mapper.writeValue(file.toFile(), entry);
        return entry;
    }

    public void resize(String kind, String sessionId, int max) throws IOException {
        List<HistoryEntry> entries = list(kind, sessionId);
        if (entries.size() > max) {
            for (HistoryEntry e : entries.subList(max, entries.size())) {
                Files.deleteIfExists(dir(kind, sessionId).resolve(e.id() + ".json"));
            }
        }
    }

    public void deleteOne(String kind, String sessionId, String id) throws IOException {
        Files.deleteIfExists(dir(kind, sessionId).resolve(id + ".json"));
    }

    public void clearAll(String kind, String sessionId) throws IOException {
        Path dir = dir(kind, sessionId);
        if (!Files.exists(dir)) return;
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                  .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }

    private Path dir(String kind, String sessionId) {
        String safeKind = "simulate".equals(kind) ? "simulate" : "generate";
        String safeSession = sessionId.replaceAll("[^a-zA-Z0-9\\-]", "_");
        return BASE_DIR.resolve(safeKind).resolve(safeSession);
    }
}
