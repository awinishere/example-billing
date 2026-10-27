package applications.crypto.features.matching;

import applications.crypto.api.MatchingContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MatchingService implements MatchingContract {

    private final PasswordEncoder passwordEncoder;

    public MatchingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public MatchingResult matches(MatchingCommand command) {
        try {
            boolean matched = passwordEncoder.matches(command.plainText(), command.textHashing());
            if (log.isDebugEnabled()) {
                log.debug("Argon2 match result={}", matched);
            }
            return new MatchingResult(matched);
        } catch (IllegalArgumentException ex) {
            log.error("Malformed Argon2 hash, cannot verify: {}", ex.getMessage(), ex);
            return new MatchingResult(false);
        }
    }
}
