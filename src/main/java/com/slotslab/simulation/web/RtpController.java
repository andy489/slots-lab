package com.slotslab.simulation.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotslab.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Simulation", description = "RTP simulation and spin testing")
@RestController
@RequestMapping("/api/rtp")
public class RtpController {

    private final RtpSimulationService simulationService;
    private final ObjectMapper objectMapper;

    public RtpController(RtpSimulationService simulationService, ObjectMapper objectMapper) {
        this.simulationService = simulationService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Run RTP simulation",
            description = "Runs a multi-threaded statistical RTP simulation (up to 8 threads). " +
                    "Returns RTP%, variance, volatility index/label, hit rate, median/max win, and per-symbol-combination breakdown.")
    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse> simulate(@RequestBody RtpRequest request) {
        try {
            RtpResult result = simulationService.simulate(request);
            return ResponseEntity.ok(ApiResponse.ok(objectMapper.writeValueAsString(result)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.err(e.getMessage()));
        }
    }
}

