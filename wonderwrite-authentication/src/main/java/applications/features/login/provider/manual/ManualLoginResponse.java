package applications.features.login.provider.manual;

import java.util.UUID;

public record ManualLoginResponse(
        UUID id,
        String email,
        boolean otpRequired,
        Integer otpTtlSeconds
) {
    public static ManualLoginResponse from(ManualLoginResult.Success success) {
        return new ManualLoginResponse(
                success.id(),
                success.email(),
                false,
                null
        );
    }

    public static ManualLoginResponse from(ManualLoginResult.OtpChallenge challenge) {
        return new ManualLoginResponse(
                challenge.userId(),
                challenge.email(),
                true,
                challenge.ttlSeconds()
        );
    }
}
