package org.evo.reels.reel;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Output {
    file,
    stdout,
    @JsonEnumDefaultValue UNKNOWN;
}
