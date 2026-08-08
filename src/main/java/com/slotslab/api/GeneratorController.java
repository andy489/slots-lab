package com.slotslab.api;

import com.slotslab.service.ConverterService;
import com.slotslab.service.GeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Generation", description = "Reel strip generation and format conversion")
@RestController
@RequestMapping("/api")
public class GeneratorController {

    private final GeneratorService generatorService;
    private final ConverterService converterService;

    public GeneratorController(GeneratorService generatorService, ConverterService converterService) {
        this.generatorService = generatorService;
        this.converterService = converterService;
    }

    @Operation(summary = "Generate reel strips",
            description = "Generates reel strip arrays from a ReelSetsCollectionData config using SHUFFLE or FLAT strategy. " +
                    "Returns a JSON array of named reel sets.")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse> generate(@RequestBody GenerateRequest request) {
        try {
            String result = generatorService.generate(request.config());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.err(e.getMessage()));
        }
    }

    @Operation(summary = "Convert reel set format",
            description = "Converts reel sets between COUNT (symbol counts per reel), JSON_ARRAY (flat strip arrays), and CSV formats.")
    @PostMapping("/convert")
    public ResponseEntity<ApiResponse> convert(@RequestBody ConvertRequest request) {
        try {
            String result = converterService.convert(request);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.err(e.getMessage()));
        }
    }
}

