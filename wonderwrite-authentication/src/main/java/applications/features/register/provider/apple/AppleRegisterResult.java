package applications.features.register.provider.apple;

import applications.extensions.types.RoleAccount;

import java.util.UUID;

public record AppleRegisterResult(
        UUID userId,
        String email,
        RoleAccount role,
        boolean provisioned
) {
}
