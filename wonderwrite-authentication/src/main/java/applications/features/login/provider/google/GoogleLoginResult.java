package applications.features.login.provider.google;

import applications.utils.tokens.*;

import java.util.UUID;

public sealed interface GoogleLoginResult {

    record Success(
            UUID id,
            TokenPair tokens,
            boolean provisioned
    ) implements GoogleLoginResult {}

    record Failed(
            String reason
    ) implements GoogleLoginResult {}
}
