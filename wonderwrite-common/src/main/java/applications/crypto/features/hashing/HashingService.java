package applications.crypto.features.hashing;

import applications.crypto.api.HashingContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HashingService implements HashingContract {

    private final PasswordEncoder passwordEncoder;

    public HashingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public HashingResult hash(HashingCommand command) {
        HashingResult result = new HashingResult(passwordEncoder.encode(command.plainText()));
        if (log.isDebugEnabled()) {
            log.debug("Argon2 hash generated for input of {} chars", command.plainText().length());
        }
        return result;
    }
}
