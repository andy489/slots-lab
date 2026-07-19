package com.slotslab.keepalive;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class KeepAliveService {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveService.class);

    @Value("${RENDER_EXTERNAL_URL:}")
    private String renderExternalUrl;

    @Value("${keep-alive.interval-seconds:840}")
    private int intervalSeconds;

    @Value("${keep-alive.initial-delay-seconds:15}")
    private int initialDelaySeconds;

    private volatile boolean running = false;
    private Thread thread;

    @PostConstruct
    public void start() {
        String render = System.getenv("RENDER");
        if (render == null || render.isBlank() || renderExternalUrl.isBlank()) {
            log.info("[KeepAlive] Not on Render — keep-alive disabled");
            return;
        }
        String url = renderExternalUrl.stripTrailing() + "/keep-alive";
        running = true;
        thread = Thread.ofVirtual().name("keep-alive").start(() -> loop(url));
        log.info("[KeepAlive] Started — pinging {} every {}s", url, intervalSeconds);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    private void loop(String url) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        try {
            Thread.sleep(Duration.ofSeconds(initialDelaySeconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        while (running) {
            ping(client, url);
            try {
                Thread.sleep(Duration.ofSeconds(intervalSeconds));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void ping(HttpClient client, String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<Void> res = client.send(req, HttpResponse.BodyHandlers.discarding());
            if (res.statusCode() == 200) {
                log.info("[KeepAlive] Ping OK ({})", res.statusCode());
            } else {
                log.warn("[KeepAlive] Ping returned {}", res.statusCode());
            }
        } catch (Exception e) {
            log.warn("[KeepAlive] Ping failed — {}", e.getMessage());
        }
    }
}
