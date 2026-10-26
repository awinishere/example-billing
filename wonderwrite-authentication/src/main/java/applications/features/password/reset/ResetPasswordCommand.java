package applications.features.reset.password;

public record ResetPasswordCommand(
        String email,
        String code,
        String newPassword
) {
}
