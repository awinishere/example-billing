package applications.features.otp.verification;

import java.util.UUID;

public record VerifyLoginOtpResponse(
        UUID id,
        String email
) {
    public static VerifyLoginOtpResponse from(VerifyLoginOtpResult.Success success) {
        return new VerifyLoginOtpResponse(
                success.id(),
                success.email()
        );
    }
}
