package com.slotslab.service;

import com.slotslab.api.ConvertRequest;
import com.slotslab.convert.CsvConverter;
import com.slotslab.convert.CountConverter;
import com.slotslab.convert.JsonArrayConverter;
import org.springframework.stereotype.Service;

@Service
public class ConverterService {

    public String convert(ConvertRequest req) {
        return switch (req.toCom()) {
            case COUNT      -> CountConverter.convert(req.reelSets());
            case JSON_ARRAY -> JsonArrayConverter.convert(req.reelSets());
            case CSV        -> CsvConverter.convert(req.gameId(), req.reelSets());
            default -> throw new IllegalArgumentException("Unknown converter: " + req.toCom());
        };
    }
}
