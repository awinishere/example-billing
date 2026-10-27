package applications.features.otp.send;

import applications.extensions.types.OneTimePassPurpose;

public sealed interface SendOtpResult {

    record Success(
            String email,
            OneTimePassPurpose purpose,
            int ttlSeconds
    ) implements SendOtpResult {}

    record Failed(
            String reason
    ) implements SendOtpResult {}
}
