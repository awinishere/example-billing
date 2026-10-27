package applications.features.otp.verification;

import applications.domain.entities.Credentials;
import applications.domain.repositories.CredentialsRepositories;
import applications.extensions.types.OneTimePassPurpose;
import applications.utils.tokens.RefreshTokenIssuer;
import applications.utils.tokens.StartTokenFamilyCommand;
import applications.utils.tokens.TokenPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyLoginOtpService {

    private final CredentialsRepositories credentialsRepositories;
    private final VerifyOtpService verifyOtpService;
    private final RefreshTokenIssuer refreshTokenIssuer;

    @Transactional
    public VerifyLoginOtpResult execute(VerifyLoginOtpCommand command) {
        Optional<Credentials> maybeUser = credentialsRepositories.findById(command.userId());

        if (maybeUser.isEmpty()) {
            return new VerifyLoginOtpResult.Failed("Invalid or expired code");
        }

        Credentials user = maybeUser.get();

        return switch (verifyOtpService.execute(new VerifyOtpCommand(
                user.getId(),
                OneTimePassPurpose.LOGIN_VERIFICATION,
                command.code()
        ))) {
            case VerifyOtpResult.Failed failed -> new VerifyLoginOtpResult.Failed(failed.reason());
            case VerifyOtpResult.Success success -> {
                user.markEmailAsVerified();
                credentialsRepositories.save(user);

                TokenPair tokens = refreshTokenIssuer.startFamily(new StartTokenFamilyCommand(
                        user.getId(),
                        user.getEmail(),
                        user.getRole(),
                        command.fingerprint()
                )).tokens();

                if (log.isDebugEnabled()) {
                    log.debug("Email verified via OTP user={}", user.getId());
                }

                yield new VerifyLoginOtpResult.Success(user.getId(), user.getEmail(), tokens);
            }
        };
    }
}
