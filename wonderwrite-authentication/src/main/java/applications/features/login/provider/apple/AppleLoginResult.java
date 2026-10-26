package applications.features.login.provider.apple;

import applications.tokens.TokenPair;

import java.util.UUID;

public record AppleLoginResult(
        UUID userId,
        TokenPair tokens,
        boolean provisioned
) {
}
