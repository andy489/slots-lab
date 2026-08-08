package com.slotslab;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Slots Lab API")
                .version("1.0")
                .description("""
                        REST API for slot machine reel strip generation, RTP simulation, spin testing, and format conversion.

                        All endpoints accept and return JSON. Successful responses use the envelope:
                        `{ "result": "<JSON string>", "error": null }`.
                        Errors return HTTP 400 with `{ "result": null, "error": "<message>" }`.

                        History endpoints are scoped per browser session via the `slotlab-session` cookie set on `GET /`.
                        """)
                .contact(new Contact().url("https://github.com/andy489/Slots_Lab")));
    }
}
