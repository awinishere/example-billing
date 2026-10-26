package applications.features.login.provider.google;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank
        @Size(max = 255)
        String providerUserId,

        @Email
        @Size(max = 320)
        String email
) {
}
