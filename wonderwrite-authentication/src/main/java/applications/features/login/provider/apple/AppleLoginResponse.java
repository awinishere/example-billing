package applications.features.login.provider.apple;

import java.util.UUID;

public record AppleLoginResponse(
        UUID id,
        boolean provisioned
) {
    public static AppleLoginResponse from(AppleLoginResult.Success success) {
        return new AppleLoginResponse(
                success.id(),
                success.provisioned()
        );
    }
}
