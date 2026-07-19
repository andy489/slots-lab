package com.slotslab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SlotsLabApp {
    public static void main(String[] args) {
        SpringApplication.run(SlotsLabApp.class, args);
    }
}
