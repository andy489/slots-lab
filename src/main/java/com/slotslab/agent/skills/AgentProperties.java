package com.slotslab.agent.skills;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private int maxAttempts = 3;
    private String skillsPath = "skills/";
}
