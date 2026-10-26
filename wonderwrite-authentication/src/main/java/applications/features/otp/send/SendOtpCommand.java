package applications.features.otp.send;

import applications.domain.entities.Credentials;
import applications.extensions.types.OneTimePassPurpose;

public record SendOtpCommand(
        Credentials credentials,
        OneTimePassPurpose purpose
) {
}
