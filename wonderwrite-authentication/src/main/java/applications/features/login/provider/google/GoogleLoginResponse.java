package applications.features.login.provider.google;

import java.util.UUID;

public record GoogleLoginResponse(
        UUID id,
        boolean provisioned
) {
    public static GoogleLoginResponse from(GoogleLoginResult.Success success) {
        return new GoogleLoginResponse(
                success.id(),
                success.provisioned()
        );
    }
}
