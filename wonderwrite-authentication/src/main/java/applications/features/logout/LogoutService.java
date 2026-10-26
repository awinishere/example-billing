package applications.features.logout;

import applications.utils.tokens.RefreshTokenIssuer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class LogoutService {

    private final RefreshTokenIssuer refreshTokenIssuer;

    @Transactional
    public LogoutResult execute(LogoutCommand command) {
        UUID userId = command.userId();
        if (userId == null) {
            return new LogoutResult.Failed("No authenticated session");
        }

        int revoked = refreshTokenIssuer.revokeAllSessions(userId);
        log.debug("Logout user={} sessionsRevoked={}", userId, revoked);
        return new LogoutResult.Success(revoked);
    }
}
