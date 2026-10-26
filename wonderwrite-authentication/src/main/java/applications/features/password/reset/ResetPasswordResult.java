package applications.features.reset.password;

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
