package com.slotslab.history;

import com.slotslab.ui.SessionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "History", description = "Per-session generation and simulation history. Requires the slotlab-session cookie (set automatically on GET /).")
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService service;

    public HistoryController(HistoryService service) {
        this.service = service;
    }

    @Operation(summary = "List history entries", description = "Returns all entries for the session, newest first. kind = generate | simulate")
    @GetMapping("/{kind}")
    public ResponseEntity<List<HistoryEntry>> list(
            @Parameter(description = "generate or simulate") @PathVariable String kind,
            HttpServletRequest req) {
        try {
            return ResponseEntity.ok(service.list(kind, SessionUtil.readSession(req)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Save a history entry")
    @PostMapping("/{kind}")
    public ResponseEntity<HistoryEntry> save(
            @Parameter(description = "generate or simulate") @PathVariable String kind,
            @RequestBody HistoryEntry entry,
            HttpServletRequest req) {
        try {
            return ResponseEntity.ok(service.save(kind, SessionUtil.readSession(req), entry));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Trim history to max N entries", description = "Deletes oldest entries beyond the given size (clamped to 1–20).")
    @PutMapping("/{kind}/resize")
    public ResponseEntity<List<HistoryEntry>> resize(
            @Parameter(description = "generate or simulate") @PathVariable String kind,
            @RequestParam int size,
            HttpServletRequest req) {
        try {
            String sid = SessionUtil.readSession(req);
            service.resize(kind, sid, Math.max(1, Math.min(20, size)));
            return ResponseEntity.ok(service.list(kind, sid));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Delete one history entry")
    @DeleteMapping("/{kind}/{id}")
    public ResponseEntity<List<HistoryEntry>> deleteOne(
            @Parameter(description = "generate or simulate") @PathVariable String kind,
            @Parameter(description = "Entry timestamp ID") @PathVariable String id,
            HttpServletRequest req) {
        try {
            String sid = SessionUtil.readSession(req);
            service.deleteOne(kind, sid, id);
            return ResponseEntity.ok(service.list(kind, sid));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Clear all history entries")
    @DeleteMapping("/{kind}")
    public ResponseEntity<Void> clearAll(
            @Parameter(description = "generate or simulate") @PathVariable String kind,
            HttpServletRequest req) {
        try {
            service.clearAll(kind, SessionUtil.readSession(req));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

