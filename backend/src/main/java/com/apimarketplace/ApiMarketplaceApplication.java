package com.apimarketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiMarketplaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiMarketplaceApplication.class, args);
    }
}
