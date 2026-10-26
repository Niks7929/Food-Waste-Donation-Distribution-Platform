package com.foodwaste.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FoodWastePlatformApplication {

    private static final Logger log = LoggerFactory.getLogger(FoodWastePlatformApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(FoodWastePlatformApplication.class, args);
        String port = context.getEnvironment().getProperty("server.port", "8081");

        System.out.println("\n" + "=".repeat(65));
        System.out.println("🚀  Food Waste Donation Platform is LIVE & READY!");
        System.out.println("🌐  Open Landing Page:  http://localhost:" + port + "/");
        System.out.println("🔐  Open Login Page:    http://localhost:" + port + "/login");
        System.out.println("=".repeat(65) + "\n");

        log.info("Platform initialized successfully on http://localhost:{}/", port);
    }
}
