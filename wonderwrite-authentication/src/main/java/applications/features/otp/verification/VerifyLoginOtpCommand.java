package applications.features.otp.verification;

import applications.domain.model.ClientFingerprint;

import java.util.UUID;

public record VerifyLoginOtpCommand(
        UUID userId,
        String code,
        ClientFingerprint fingerprint
) {
}
