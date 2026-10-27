package applications.utils.tokens;

import applications.extensions.types.RoleAccount;

import java.util.UUID;

public record IssueTokenCommand(
        UUID userId,
        String email,
        RoleAccount role
) {
    public IssueTokenCommand {
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
