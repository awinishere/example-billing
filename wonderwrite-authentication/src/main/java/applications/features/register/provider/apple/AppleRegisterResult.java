package applications.features.register.provider.apple;

import applications.extensions.types.RoleAccount;

import java.util.UUID;

public sealed interface AppleRegisterResult {

    record Success(
            UUID id,
            String email,
            RoleAccount role,
            boolean provisioned
    ) implements AppleRegisterResult {}

    record Failed(
            String reason
    ) implements AppleRegisterResult {}
}
