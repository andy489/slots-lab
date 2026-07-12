package com.slotslab.convert;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum ConverterType {
    COUNT,
    JSON_ARRAY,
    CSV,
    @JsonEnumDefaultValue UNKNOWN;
}
