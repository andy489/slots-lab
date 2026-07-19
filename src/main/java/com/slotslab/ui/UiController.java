package com.slotslab.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UiController {

    @GetMapping("/")
    public String index(HttpServletRequest req, HttpServletResponse res) {
        SessionUtil.ensureSession(req, res);
        return "index";
    }

    @GetMapping("/keep-alive")
    @ResponseBody
    public ResponseEntity<String> keepAlive() {
        return ResponseEntity.ok("ok");
    }
}
