package applications.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private Path privateKeyPath;
    private Path publicKeyPath;
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private String issuer = "wonderwrite";

    @PostConstruct
    void validate() {
        if (privateKeyPath == null || !Files.isReadable(privateKeyPath)) {
            throw new IllegalStateException(
                    "app.security.jwt.private-key-path must point to a readable PKCS#8 .pem file, set JWT_PRIVATE_KEY_PATH env var and never hard-code it"
            );
        }

        if (publicKeyPath == null || !Files.isReadable(publicKeyPath)) {
            throw new IllegalStateException(
                    "app.security.jwt.public-key-path must point to a readable X.509 .pem file, set JWT_PUBLIC_KEY_PATH env var and never hard-code it"
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
