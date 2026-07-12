package com.slotlab.reels.api;

import com.slotlab.reels.service.ConverterService;
import com.slotlab.reels.service.GeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GeneratorController {

    private final GeneratorService generatorService;
    private final ConverterService converterService;

    public GeneratorController(GeneratorService generatorService, ConverterService converterService) {
        this.generatorService = generatorService;
        this.converterService = converterService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse> generate(@RequestBody GenerateRequest request) {
        try {
            String result = generatorService.generate(request.config());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.err(e.getMessage()));
        }
    }

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
