package applications.features.password.reset;

import java.util.UUID;

public record ResetPasswordResponse(
        UUID id
) {
    public static ResetPasswordResponse from(ResetPasswordResult.Success success) {
        return new ResetPasswordResponse(success.id());
    }
}
