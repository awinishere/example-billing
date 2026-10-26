package applications.features.reset.password;

import java.util.UUID;

public record ResetPasswordResponse(
        UUID id
) {
    public static ResetPasswordResponse from(ResetPasswordResult.Success success) {
        return new ResetPasswordResponse(success.id());
    }
}
