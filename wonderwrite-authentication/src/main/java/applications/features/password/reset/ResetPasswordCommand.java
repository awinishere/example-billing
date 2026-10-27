package applications.features.password.reset;

public record ResetPasswordCommand(
        String email,
        String code,
        String newPassword
) {
}
