package applications.features.otp.verification;

import applications.utils.tokens.TokenPair;

import java.util.UUID;

public sealed interface VerifyLoginOtpResult {

    record Success(
            UUID id,
            String email,
            TokenPair tokens
    ) implements VerifyLoginOtpResult {}

    record Failed(
            String reason
    ) implements VerifyLoginOtpResult {}
}
