package org.evo.reels.wrapper;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.evo.reels.convert.ConverterType;

public record ConvertWrapper(
        @JsonProperty("confirm") @JsonAlias({"do", "convert"}) boolean confirm,
        @JsonProperty("toCom")   @JsonAlias({"com", "formatType", "format"}) ConverterType toCom,
        @JsonProperty("src")     String src,
        @JsonProperty("dest")    String dest
) {}
