package applications.features.login.provider.manual;

import applications.domain.model.ClientFingerprint;

public record ManualLoginCommand(
        String email,
        String password,
        ClientFingerprint fingerprint
) {
}
