package com.slotslab.rtp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotslab.api.ApiResponse;
import com.slotslab.dto.SpinData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spin-test")
public class SpinTestController {

    private final SpinTestService spinTestService;
    private final ObjectMapper objectMapper;

    public SpinTestController(SpinTestService spinTestService, ObjectMapper objectMapper) {
        this.spinTestService = spinTestService;
        this.objectMapper = objectMapper;
    }

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
