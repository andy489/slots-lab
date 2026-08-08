package com.slotslab.ui;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tabs")
public class TabsProperties {

    private boolean generate   = true;
    private boolean rtp        = true;
    private boolean spinTest   = true;
    private boolean convert    = true;
    private boolean io         = true;
    private boolean ai         = false;
}
