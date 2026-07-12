package com.slotslab.reel;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Strategy {
    SHUFFLE,
    FLAT,
    @JsonEnumDefaultValue UNKNOWN;
}
