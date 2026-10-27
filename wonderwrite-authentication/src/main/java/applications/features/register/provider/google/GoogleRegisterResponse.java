package applications.features.register.provider.google;

import java.util.UUID;

public record GoogleRegisterResponse(
        UUID id,
        String email,
        boolean provisioned
) {
    public static GoogleRegisterResponse from(GoogleRegisterResult.Success success) {
        return new GoogleRegisterResponse(
                success.id(),
                success.email(),
                success.provisioned()
        );
    }
}
