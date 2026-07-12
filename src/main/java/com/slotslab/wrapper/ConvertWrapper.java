package com.slotslab.wrapper;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.slotslab.convert.ConverterType;

public record ConvertWrapper(
        @JsonProperty("confirm") @JsonAlias({"do", "convert"}) boolean confirm,
        @JsonProperty("toCom")   @JsonAlias({"com", "formatType", "format"}) ConverterType toCom,
        @JsonProperty("src")     String src,
        @JsonProperty("dest")    String dest
) {}
