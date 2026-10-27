package applications.features.register.provider.apple;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppleRegisterRequest(
        @NotBlank
        @Size(max = 255)
        String providerUserId,

        @Email
        @Size(max = 320)
        String email
) {
    public AppleRegisterCommand toCommand() {
        return new AppleRegisterCommand(providerUserId, email);
    }
}
