package applications.features.login.provider.google;

import applications.domain.model.ClientFingerprint;

public record GoogleLoginCommand(
        String providerUserId,
        String email,
        ClientFingerprint fingerprint
) {
    public GoogleLoginCommand {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("Google subject must not be blank");
        }
        providerUserId = providerUserId.trim();
    }
}
