package applications.features.otp.verification;

import applications.extensions.types.OneTimePassPurpose;

import java.util.UUID;

public record VerifyOtpCommand(
        UUID userId,
        OneTimePassPurpose purpose,
        String code
) {
}
