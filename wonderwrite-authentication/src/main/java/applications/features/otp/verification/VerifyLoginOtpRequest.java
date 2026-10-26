package applications.features.otp.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VerifyLoginOtpRequest(
        @NotNull
        UUID userId,

        @NotBlank
        @Size(min = 4, max = 10)
        String code
) {
}
