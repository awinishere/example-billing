package applications;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(
        title = "wonderwrite-authentication",
        version = "0.1.0",
        description = """
                Identity service for Wonderwrite: account registration, credential storage and login token lifecycle.

                Covers manual (email + password), Google and Apple sign-in, OTP email verification with step-up login,
                and forgot/reset password flows.

                Authentication model:
                - Successful login issues an RS256-signed JWT access token and a refresh token, both delivered as
                  HttpOnly, Secure, SameSite=Strict cookies (access_token path=/, refresh_token path=/api/v1/refresh).
                - Response bodies never contain token values; the tokens travel only in the Set-Cookie headers.
                - All endpoints below are public by design (they establish the session); the security schemes are
                  documented so downstream services can reuse the same token format.

                Rate limiting:
                - Failed manual logins are guarded per email and per IP. Exceeding the threshold returns 429 with a
                  Retry-After header (seconds until the window clears).
                """,
        contact = @Contact(name = "awinishere", url = "https://github.com/awinishere/example-billing"),
        license = @License(name = "Proprietary")))
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
