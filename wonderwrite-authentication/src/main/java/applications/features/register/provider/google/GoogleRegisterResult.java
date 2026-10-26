package applications.features.register.provider.google;

import applications.extensions.types.RoleAccount;

import java.util.UUID;

public sealed interface GoogleRegisterResult {

    record Success(
            UUID id,
            String email,
            RoleAccount role,
            boolean provisioned
    ) implements GoogleRegisterResult {}

    record Failed(
            String reason
    ) implements GoogleRegisterResult {}
}
