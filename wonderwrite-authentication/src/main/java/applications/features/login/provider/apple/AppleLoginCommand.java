package applications.features.login.provider.apple;

import applications.domain.model.ClientFingerprint;

public record AppleLoginCommand(
        String providerUserId,
        String email,
        ClientFingerprint fingerprint
) {
    public AppleLoginCommand {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("Apple subject must not be blank");
        }
        providerUserId = providerUserId.trim();
    }
}
