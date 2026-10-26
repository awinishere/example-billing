package applications.features.reset.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(min = 4, max = 10)
        String code,

        @NotBlank
        @Size(max = 128)
        String newPassword
) {
    public ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(email, code, newPassword);
    }
}
