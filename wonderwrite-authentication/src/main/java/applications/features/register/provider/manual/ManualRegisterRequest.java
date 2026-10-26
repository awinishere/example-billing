package applications.features.register.provider.manual;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManualRegisterRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 128)
        String password,

        @NotBlank
        @Size(max = 128)
        String confirmPassword
) {
    public ManualRegisterCommand toCommand() {
        return new ManualRegisterCommand(email, password, confirmPassword);
    }
}
