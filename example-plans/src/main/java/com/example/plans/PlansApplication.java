package com.example.plans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PlansApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlansApplication.class, args);
    }
}
