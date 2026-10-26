package applications.features.login.provider.google;

import applications.tokens.TokenPair;

import java.util.UUID;

public record GoogleLoginResult(
        UUID userId,
        TokenPair tokens,
        boolean provisioned
) {
}
