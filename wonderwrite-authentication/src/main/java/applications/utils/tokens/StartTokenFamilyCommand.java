package applications.utils.tokens;

import applications.domain.model.ClientFingerprint;
import applications.extensions.types.RoleAccount;

import java.util.UUID;

public record StartTokenFamilyCommand(
        UUID userId,
        String email,
        RoleAccount role,
        ClientFingerprint fingerprint
) {
    public StartTokenFamilyCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
    }
}
