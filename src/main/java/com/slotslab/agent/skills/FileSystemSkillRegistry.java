package com.slotslab.agent.skills;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Component
public class FileSystemSkillRegistry implements SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(FileSystemSkillRegistry.class);

    private final AgentProperties properties;
    private final Map<String, Skill> cache = new ConcurrentHashMap<>();

    public FileSystemSkillRegistry(AgentProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void loadAll() {
        Path skillsDir = resolveSkillsPath();
        if (!Files.isDirectory(skillsDir)) {
            log.warn("Skills directory '{}' does not exist — no skills loaded", skillsDir);
            return;
        }
        try (Stream<Path> stream = Files.walk(skillsDir)) {
            stream.filter(p -> p.toString().endsWith(".md"))
                  .forEach(this::loadSkill);
        } catch (IOException e) {
            log.error("Failed to walk skills directory '{}': {}", skillsDir, e.getMessage());
        }
        log.info("Loaded {} skill(s) from '{}'", cache.size(), skillsDir);
    }

    private void loadSkill(Path path) {
        try {
            String content = Files.readString(path);
            String id = deriveId(path);
            String name = extractFirstHeading(content, id);
            String description = extractDescription(content);
            Skill skill = new Skill(id, name, description, path, content);
            cache.put(id, skill);
            log.debug("Loaded skill '{}' from '{}'", id, path);
        } catch (IOException e) {
            log.warn("Could not load skill from '{}': {}", path, e.getMessage());
        }
    }

    private String deriveId(Path path) {
        Path skillsDir = resolveSkillsPath();
        Path relative = skillsDir.relativize(path);
        String rel = relative.toString().replace('\\', '/');
        int dot = rel.lastIndexOf('.');
        return dot >= 0 ? rel.substring(0, dot) : rel;
    }

    private String extractFirstHeading(String content, String fallback) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
        }
        return fallback;
    }

    private String extractDescription(String content) {
        boolean inPurpose = false;
        StringBuilder sb = new StringBuilder();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase("## Purpose")) { inPurpose = true; continue; }
            if (inPurpose) {
                if (trimmed.startsWith("##")) break;
                if (!trimmed.isEmpty()) sb.append(trimmed).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private Path resolveSkillsPath() {
        return Paths.get(properties.getSkillsPath()).toAbsolutePath();
    }

    @Override
    public Skill getSkill(String skillId) {
        return cache.get(skillId);
    }

    @Override
    public List<Skill> getAvailableSkills() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public List<Skill> getSkillsForStrategy(String strategy) {
        String prefix = strategy.toLowerCase() + "/";
        return cache.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().startsWith(prefix))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(Skill::id))
                .toList();
    }
}
