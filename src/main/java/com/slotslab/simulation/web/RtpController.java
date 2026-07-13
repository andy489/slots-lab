package com.slotslab.simulation.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotslab.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rtp")
public class RtpController {

    private final RtpSimulationService simulationService;
    private final ObjectMapper objectMapper;

    public RtpController(RtpSimulationService simulationService, ObjectMapper objectMapper) {
        this.simulationService = simulationService;
        this.objectMapper = objectMapper;
    }

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
