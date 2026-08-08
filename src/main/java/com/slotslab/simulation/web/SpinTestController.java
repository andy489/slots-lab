package com.slotslab.simulation.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotslab.api.ApiResponse;
import com.slotslab.dto.spin.SpinData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Simulation", description = "RTP simulation and spin testing")
@RestController
@RequestMapping("/api/spin-test")
public class SpinTestController {

    private final SpinTestService spinTestService;
    private final ObjectMapper objectMapper;

    public SpinTestController(SpinTestService spinTestService, ObjectMapper objectMapper) {
        this.spinTestService = spinTestService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Evaluate individual spins",
            description = "Evaluates 1–N spins with full per-line/ways/cluster payout detail. " +
                    "Supports fixed screen input, fixed reel stops, or random draws. " +
                    "Returns a list of SpinData objects each containing screen, stops, reel heights (MEGAWAYS), and payout breakdown.")
    @PostMapping
    public ResponseEntity<ApiResponse> spinTest(@RequestBody SpinTestRequest request) {
        try {
            List<SpinData> results = spinTestService.generate(request);
            return ResponseEntity.ok(ApiResponse.ok(objectMapper.writeValueAsString(results)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.err(e.getMessage()));
        }
    }
}

