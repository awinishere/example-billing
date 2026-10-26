package applications.features.password.forgot;

import applications.domain.entities.Credentials;
import applications.domain.repositories.CredentialsRepositories;
import applications.extensions.types.OneTimePassPurpose;
import applications.features.otp.send.SendOtpCommand;
import applications.features.otp.send.SendOtpResult;
import applications.features.otp.send.SendOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final CredentialsRepositories credentialsRepositories;
    private final SendOtpService sendOtpService;

    @Transactional
    public ForgotPasswordResult execute(ForgotPasswordCommand command) {
        String email = normalize(command.email());
        Optional<Credentials> maybeUser = credentialsRepositories.findByEmail(email);

        if (maybeUser.isEmpty()) {
            log.debug("Forgot-password for unknown email (no OTP dispatched)");
            return new ForgotPasswordResult.Success(false, 0);
        }

        return switch (sendOtpService.execute(new SendOtpCommand(
                maybeUser.get(),
                OneTimePassPurpose.PASSWORD_RESET
        ))) {
            case SendOtpResult.Success success -> new ForgotPasswordResult.Success(true, success.ttlSeconds());
            case SendOtpResult.Failed failed -> {
                log.debug("Password-reset OTP resend blocked reason={}", failed.reason());
                yield new ForgotPasswordResult.Success(false, 0);
            }
        };
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
