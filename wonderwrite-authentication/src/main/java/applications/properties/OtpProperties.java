package applications.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.otp")
public class OtpProperties {

    private Duration ttl = Duration.ofMinutes(5);
    private Duration resendGap = Duration.ofMinutes(1);
    private int maxAttempts = 3;
    private int codeLength = 6;

    @PostConstruct
    void validate() {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("app.security.otp.ttl must be positive");
        }

        if (resendGap == null || resendGap.isNegative()) {
            throw new IllegalStateException("app.security.otp.resend-gap must not be negative");
        }

        if (maxAttempts < 1) {
            throw new IllegalStateException("app.security.otp.max-attempts must be at least 1");
        }

        if (codeLength < 4 || codeLength > 10) {
            throw new IllegalStateException("app.security.otp.code-length must be between 4 and 10");
        }
    }
}
