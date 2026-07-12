package com.slotslab.reels.history;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService service;

    public HistoryController(HistoryService service) {
        this.service = service;
    }

    @GetMapping("/{kind}")
    public ResponseEntity<List<HistoryEntry>> list(@PathVariable String kind) {
        try {
            return ResponseEntity.ok(service.list(kind));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{kind}")
    public ResponseEntity<HistoryEntry> save(@PathVariable String kind, @RequestBody HistoryEntry entry) {
        try {
            return ResponseEntity.ok(service.save(kind, entry));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{kind}/resize")
    public ResponseEntity<List<HistoryEntry>> resize(@PathVariable String kind, @RequestParam int size) {
        try {
            service.resize(kind, Math.max(1, Math.min(20, size)));
            return ResponseEntity.ok(service.list(kind));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{kind}/{id}")
    public ResponseEntity<List<HistoryEntry>> deleteOne(@PathVariable String kind, @PathVariable String id) {
        try {
            service.deleteOne(kind, id);
            return ResponseEntity.ok(service.list(kind));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{kind}")
    public ResponseEntity<Void> clearAll(@PathVariable String kind) {
        try {
            service.clearAll(kind);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
