package applications.features.login.provider.manual;

import applications.utils.tokens.TokenPair;

import java.util.UUID;

public sealed interface ManualLoginResult {

    record Success(
            UUID id,
            String email,
            TokenPair tokens
    ) implements ManualLoginResult {}

    record OtpChallenge(
            UUID userId,
            String email,
            int ttlSeconds
    ) implements ManualLoginResult {}

    record Failed(
            String reason
    ) implements ManualLoginResult {}
}
