package applications.features.otp.verification;

import applications.extensions.types.OneTimePassPurpose;

import java.util.UUID;

public sealed interface VerifyOtpResult {

    record Success(
            UUID userId,
            OneTimePassPurpose purpose
    ) implements VerifyOtpResult {}

    record Failed(
            String reason
    ) implements VerifyOtpResult {}
}
