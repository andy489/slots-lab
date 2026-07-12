package com.slotlab.reels.convert;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum ConverterType {
    COUNT,
    JSON_ARRAY,
    CSV,
    @JsonEnumDefaultValue UNKNOWN;
}
