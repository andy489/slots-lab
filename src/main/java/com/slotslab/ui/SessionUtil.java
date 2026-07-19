package com.slotslab.ui;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public final class SessionUtil {

    static final String COOKIE_NAME = "slotlab-session";

    private SessionUtil() {}

    public static String ensureSession(HttpServletRequest req, HttpServletResponse res) {
        String existing = readSession(req);
        if (!"anonymous".equals(existing)) return existing;
        String id = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(COOKIE_NAME, id);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60 * 24 * 365);
        res.addCookie(cookie);
        return id;
    }

    public static String readSession(HttpServletRequest req) {
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if (COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank())
                    return c.getValue();
            }
        }
        return "anonymous";
    }
}
