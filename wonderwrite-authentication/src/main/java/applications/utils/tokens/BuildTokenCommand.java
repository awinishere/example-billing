package applications.utils.tokens;

import applications.extensions.types.RoleAccount;
import applications.extensions.types.TokenType;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record BuildTokenCommand(
        UUID userId,
        String email,
        RoleAccount role,
        TokenType type,
        Instant issuedAt,
        Duration ttl
) {
}
