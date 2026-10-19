package com.example.plans;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(
        title = "example-plans",
        version = "1.0",
        description = "Plan catalogue, plan pricing, feature catalogue and plan entitlements.",
        contact = @Contact(name = "awinishere", url = "https://github.com/awinishere/example-billing")))
public class PlansApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlansApplication.class, args);
    }
}
