package applications.features.password.reset;

import java.util.UUID;

public sealed interface ResetPasswordResult {

    record Success(
            UUID id,
            int sessionsRevoked
    ) implements ResetPasswordResult {}

    record Failed(
            String reason
    ) implements ResetPasswordResult {}
}
