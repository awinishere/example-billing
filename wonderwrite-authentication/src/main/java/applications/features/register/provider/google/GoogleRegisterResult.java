package applications.features.register.provider.google;

import applications.extensions.types.RoleAccount;

import java.util.UUID;

public record GoogleRegisterResult(
        UUID userId,
        String email,
        RoleAccount role,
        boolean provisioned
) {
}
