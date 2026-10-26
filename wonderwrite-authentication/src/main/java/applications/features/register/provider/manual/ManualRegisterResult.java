package applications.features.register.provider.manual;

import java.util.UUID;

public sealed interface ManualRegisterResult {
    record Success(
            UUID id,
            String email,
            boolean emailVerification
    ) implements ManualRegisterResult {}

    record Failed(
            String reason
    ) implements ManualRegisterResult {}
}
