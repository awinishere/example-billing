package applications.features.reset.password;

import applications.crypto.api.HashingContract;
import applications.crypto.features.hashing.HashingCommand;
import applications.domain.entities.Credentials;
import applications.domain.repositories.CredentialsRepositories;
import applications.extensions.types.OneTimePassPurpose;
import applications.features.otp.verification.VerifyOtpCommand;
import applications.features.otp.verification.VerifyOtpResult;
import applications.features.otp.verification.VerifyOtpService;
import applications.utils.tokens.RefreshTokenIssuer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordService {

    private static final String GENERIC_REJECTION = "Invalid or expired reset code";

    private final CredentialsRepositories credentialsRepositories;
    private final VerifyOtpService verifyOtpService;
    private final HashingContract hashingContract;
    private final RefreshTokenIssuer refreshTokenIssuer;

    @Transactional
    public ResetPasswordResult execute(ResetPasswordCommand command) {
        String email = normalize(command.email());
        Optional<Credentials> maybeUser = credentialsRepositories.findByEmail(email);

        if (maybeUser.isEmpty()) {
            log.debug("Reset-password for unknown email rejected");
            return new ResetPasswordResult.Failed(GENERIC_REJECTION);
        }

        Credentials user = maybeUser.get();

        return switch (verifyOtpService.execute(new VerifyOtpCommand(
                user.getId(),
                OneTimePassPurpose.PASSWORD_RESET,
                command.code()
        ))) {
            case VerifyOtpResult.Failed failed -> new ResetPasswordResult.Failed(GENERIC_REJECTION);
            case VerifyOtpResult.Success success -> {
                user.changePassword(hashingContract.hash(new HashingCommand(command.newPassword())).hash());
                credentialsRepositories.save(user);

                int revoked = refreshTokenIssuer.revokeAllSessions(user.getId());

                if (log.isDebugEnabled()) {
                    log.debug("Password reset complete user={} sessionsRevoked={}", user.getId(), revoked);
                }

                yield new ResetPasswordResult.Success(user.getId(), revoked);
            }
        };
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
