package applications.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.login")
public class LoginPolicyProperties {

    private int maxFailedPerEmail = 5;
    private Duration emailLockout = Duration.ofMinutes(15);
    private int maxAttemptsPerIp = 20;
    private Duration ipRateWindow = Duration.ofMinutes(1);

    @PostConstruct
    void validate() {
        if (maxFailedPerEmail < 1) {
            throw new IllegalStateException("app.security.login.max-failed-per-email must be at least 1");
        }

        if (emailLockout == null || emailLockout.isZero() || emailLockout.isNegative()) {
            throw new IllegalStateException("app.security.login.email-lockout must be positive");
        }

        if (maxAttemptsPerIp < 1) {
            throw new IllegalStateException("app.security.login.max-attempts-per-ip must be at least 1");
        }

        if (ipRateWindow == null || ipRateWindow.isZero() || ipRateWindow.isNegative()) {
            throw new IllegalStateException("app.security.login.ip-rate-window must be positive");
        }
    }
}
