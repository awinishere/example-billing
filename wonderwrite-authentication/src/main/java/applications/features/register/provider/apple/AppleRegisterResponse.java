package applications.features.register.provider.apple;

import java.util.UUID;

public record AppleRegisterResponse(
        UUID id,
        String email,
        boolean provisioned
) {
    public static AppleRegisterResponse from(AppleRegisterResult.Success success) {
        return new AppleRegisterResponse(
                success.id(),
                success.email(),
                success.provisioned()
        );
    }
}
