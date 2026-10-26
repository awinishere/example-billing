package applications.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private String secret;
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private String issuer = "wonderwrite";

    @PostConstruct
    void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.security.jwt.secret is required, set JWT_SECRET env var and never hard-code it"
            );
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be at least 32 bytes for HS256"
            );
        }

        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalStateException("app.security.jwt.access-token-ttl must be positive");
        }

        if (refreshTokenTtl == null || refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalStateException("app.security.jwt.refresh-token-ttl must be positive");
        }
    }
}
