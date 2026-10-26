package applications;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(
        title = "wonderwrite-authentication",
        version = "0.1.0",
        description = "Account registration, credential storage and login token lifecycle.",
        contact = @Contact(name = "awinishere", url = "https://github.com/awinishere/example-billing")))
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
