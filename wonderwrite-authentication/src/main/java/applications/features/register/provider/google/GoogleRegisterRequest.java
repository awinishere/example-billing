package applications.features.register.provider.google;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleRegisterRequest(
        @NotBlank
        @Size(max = 255)
        String providerUserId,

        @Email
        @Size(max = 320)
        String email
) {
    public GoogleRegisterCommand toCommand() {
        return new GoogleRegisterCommand(providerUserId, email);
    }
}
