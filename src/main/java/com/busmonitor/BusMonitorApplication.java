package com.busmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BusMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusMonitorApplication.class, args);
    }
}
