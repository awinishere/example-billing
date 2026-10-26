package applications.utils.tokens;

import applications.extensions.types.RoleAccount;
import applications.extensions.types.TokenType;

import java.util.UUID;

public record TokenClaims(
        UUID userId,
        String email,
        RoleAccount role,
        TokenType type,
        long issuedAtEpochSeconds,
        long expiresAtEpochSeconds
) {
}
