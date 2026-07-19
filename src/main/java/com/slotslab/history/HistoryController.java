package com.slotslab.history;

import com.slotslab.ui.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<List<HistoryEntry>> list(@PathVariable String kind, HttpServletRequest req) {
        try {
            return ResponseEntity.ok(service.list(kind, SessionUtil.readSession(req)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{kind}")
    public ResponseEntity<HistoryEntry> save(@PathVariable String kind, @RequestBody HistoryEntry entry, HttpServletRequest req) {
        try {
            return ResponseEntity.ok(service.save(kind, SessionUtil.readSession(req), entry));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{kind}/resize")
    public ResponseEntity<List<HistoryEntry>> resize(@PathVariable String kind, @RequestParam int size, HttpServletRequest req) {
        try {
            String sid = SessionUtil.readSession(req);
            service.resize(kind, sid, Math.max(1, Math.min(20, size)));
            return ResponseEntity.ok(service.list(kind, sid));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{kind}/{id}")
    public ResponseEntity<List<HistoryEntry>> deleteOne(@PathVariable String kind, @PathVariable String id, HttpServletRequest req) {
        try {
            String sid = SessionUtil.readSession(req);
            service.deleteOne(kind, sid, id);
            return ResponseEntity.ok(service.list(kind, sid));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{kind}")
    public ResponseEntity<Void> clearAll(@PathVariable String kind, HttpServletRequest req) {
        try {
            service.clearAll(kind, SessionUtil.readSession(req));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
