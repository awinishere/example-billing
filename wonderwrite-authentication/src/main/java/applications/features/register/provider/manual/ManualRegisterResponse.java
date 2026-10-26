package applications.features.register.provider.manual;

import java.util.UUID;

public record ManualRegisterResponse(
        UUID id,
        String email,
        boolean emailVerification
) {
    public static ManualRegisterResponse from(ManualRegisterResult.Success success) {
        return new ManualRegisterResponse(
                success.id(),
                success.email(),
                success.emailVerification()
        );
    }
}
