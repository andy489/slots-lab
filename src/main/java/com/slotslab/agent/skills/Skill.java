package com.slotslab.agent.skills;

import java.nio.file.Path;

public record Skill(String id, String name, String description, Path source, String instructions) {

}
