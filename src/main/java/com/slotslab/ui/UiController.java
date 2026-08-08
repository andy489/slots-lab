package com.slotslab.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UiController {

    private final TabsProperties tabs;

    public UiController(TabsProperties tabs) {
        this.tabs = tabs;
    }

    @GetMapping("/")
    public String index(HttpServletRequest req, HttpServletResponse res, Model model) {
        SessionUtil.ensureSession(req, res);
        model.addAttribute("tabs", tabs);
        return "index";
    }

    @GetMapping("/keep-alive")
    @ResponseBody
    public ResponseEntity<String> keepAlive() {
        return ResponseEntity.ok("ok");
    }
}
