package com.slotslab.agent;

import com.slotslab.agent.skills.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillRegistryTest {

    @TempDir
    Path tempDir;

    private AgentProperties propsFor(Path dir) {
        AgentProperties p = new AgentProperties();
        p.setSkillsPath(dir.toString());
        return p;
    }

    @Test
    void loadsMarkdownSkillSuccessfully() throws IOException {
        Path file = tempDir.resolve("my-skill.md");
        Files.writeString(file, "# My Skill\n\n## Purpose\n\nDoes something.\n\n## Strategy\n\nDo it well.\n");

        FileSystemSkillRegistry registry = new FileSystemSkillRegistry(propsFor(tempDir));
        registry.loadAll();

        Skill skill = registry.getSkill("my-skill");
        assertNotNull(skill);
        assertEquals("my-skill", skill.id());
        assertEquals("My Skill", skill.name());
        assertTrue(skill.description().contains("Does something"));
        assertNotNull(skill.instructions());
    }

    @Test
    void discoversAllMdFiles() throws IOException {
        Files.writeString(tempDir.resolve("skill-a.md"), "# A\n## Purpose\nA.\n");
        Files.writeString(tempDir.resolve("skill-b.md"), "# B\n## Purpose\nB.\n");
        Files.writeString(tempDir.resolve("notes.txt"), "not a skill");

        FileSystemSkillRegistry registry = new FileSystemSkillRegistry(propsFor(tempDir));
        registry.loadAll();

        List<Skill> skills = registry.getAvailableSkills();
        assertEquals(2, skills.size());
    }

    @Test
    void returnsNullForUnknownSkill() throws IOException {
        FileSystemSkillRegistry registry = new FileSystemSkillRegistry(propsFor(tempDir));
        registry.loadAll();

        assertNull(registry.getSkill("nonexistent"));
    }

    @Test
    void emptyDirectoryProducesNoSkills() {
        FileSystemSkillRegistry registry = new FileSystemSkillRegistry(propsFor(tempDir));
        registry.loadAll();

        assertTrue(registry.getAvailableSkills().isEmpty());
    }
}
