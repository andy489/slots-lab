package com.slotslab.ui;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UiController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/keep-alive")
    @ResponseBody
    public ResponseEntity<String> keepAlive() {
        return ResponseEntity.ok("ok");
    }
}
