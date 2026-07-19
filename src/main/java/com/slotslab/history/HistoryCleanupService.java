package com.slotslab.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class HistoryCleanupService {

    private static final Logger log = LoggerFactory.getLogger(HistoryCleanupService.class);
    private static final Path BASE_DIR = Paths.get("history");

    @Value("${history.session-ttl-days:7}")
    private int ttlDays;

    // Run once per day at 03:17
    @Scheduled(cron = "0 17 3 * * *")
    public void purgeExpiredSessions() {
        if (!Files.exists(BASE_DIR)) return;
        Instant cutoff = Instant.now().minus(ttlDays, ChronoUnit.DAYS);
        for (String kind : new String[]{"generate", "simulate"}) {
            Path kindDir = BASE_DIR.resolve(kind);
            if (!Files.isDirectory(kindDir)) continue;
            try (var sessions = Files.list(kindDir)) {
                sessions.filter(Files::isDirectory).forEach(sessionDir -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(sessionDir, BasicFileAttributes.class);
                        Instant lastModified = attrs.lastModifiedTime().toInstant();
                        if (lastModified.isBefore(cutoff)) {
                            deleteDirectory(sessionDir);
                            log.info("Purged stale history session: {}", sessionDir);
                        }
                    } catch (IOException e) {
                        log.warn("Could not inspect session dir {}: {}", sessionDir, e.getMessage());
                    }
                });
            } catch (IOException e) {
                log.warn("Could not list kind dir {}: {}", kindDir, e.getMessage());
            }
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
