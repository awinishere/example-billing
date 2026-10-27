package applications.security;

import applications.extensions.types.RoleAccount;

import java.util.UUID;

public record AuthPrincipal(
        UUID userId,
        String email,
        RoleAccount role
) {
}
