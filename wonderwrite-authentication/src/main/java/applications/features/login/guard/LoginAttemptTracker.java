package applications.features.login.guard;

import applications.domain.entities.LoginAttempt;
import applications.domain.repositories.LoginAttemptRepositories;
import applications.extensions.exception.RateLimitedException;
import applications.properties.LoginPolicyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptTracker {

    private final LoginAttemptRepositories repositories;
    private final LoginPolicyProperties policy;

    @Transactional(readOnly = true)
    public void assertAllowed(AssertLoginAllowedCommand command) {
        String email = command.email();
        String ip = command.ip();

        LocalDateTime emailSince = LocalDateTime.now().minus(policy.getEmailLockout());

        long failed = repositories.countFailedByEmailSince(email, emailSince);
        if (failed >= policy.getMaxFailedPerEmail()) {
            log.warn("Email lockout triggered email='{}' failedCount={}", maskEmail(email), failed);
            throw new RateLimitedException("Too many failed login attempts", policy.getEmailLockout());
        }

        if (ip != null && !ip.isBlank()) {
            LocalDateTime ipSince = LocalDateTime.now().minus(policy.getIpRateWindow());

            long ipAttempts = repositories.countAttemptsByIpSince(ip, ipSince);
            if (ipAttempts >= policy.getMaxAttemptsPerIp()) {
                log.warn("IP rate limit hit ip='{}' count={}", ip, ipAttempts);
                throw new RateLimitedException("Too many requests from this IP", policy.getIpRateWindow());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(RecordLoginAttemptCommand command) {
        repositories.save(LoginAttempt.newAttempt(
                command.email(),
                command.ip(),
                command.succeeded()
        ));

        if (log.isDebugEnabled()) {
            log.debug(
                    "Login attempt recorded email='{}' ip='{}' succeeded={}",
                    maskEmail(command.email()),
                    command.ip(),
                    command.succeeded()
            );
        }
    }

    private String maskEmail(String email) {
        if (email == null) {
            return "null";
        }
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
