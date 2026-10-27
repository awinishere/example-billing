package applications.features.login.provider.apple;

import applications.utils.tokens.TokenPair;

import java.util.UUID;

public sealed interface AppleLoginResult {

    record Success(
            UUID id,
            TokenPair tokens,
            boolean provisioned
    ) implements AppleLoginResult {}

    record Failed(
            String reason
    ) implements AppleLoginResult {}
}
