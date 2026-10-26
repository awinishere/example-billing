package applications.features.login.provider.manual;

import applications.crypto.api.MatchingContract;
import applications.crypto.features.matching.MatchingCommand;
import applications.domain.entities.Credentials;
import applications.domain.repositories.CredentialsRepositories;
import applications.extensions.types.OneTimePassPurpose;
import applications.features.otp.send.SendOtpCommand;
import applications.features.otp.send.SendOtpResult;
import applications.features.otp.send.SendOtpService;
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
public class ManualLoginService {

    private final CredentialsRepositories credentialsRepositories;
    private final MatchingContract matchingContract;
    private final SendOtpService sendOtpService;
    private final RefreshTokenIssuer refreshTokenIssuer;

    @Transactional
    public ManualLoginResult execute(ManualLoginCommand command) {
        String email = normalize(command.email());
        Optional<Credentials> maybeUser = credentialsRepositories.findByEmail(email);

        if (maybeUser.isEmpty() || !matchingContract.matches(new MatchingCommand(
                command.password(),
                maybeUser.get().getPassword())
        ).matched()) {
            log.debug("Login rejected email={} reason=invalid_credentials", maskEmail(email));
            return new ManualLoginResult.Failed("Invalid email or password");
        }

        Credentials user = maybeUser.get();

        if (!user.isEmailVerification()) {
            return switch (sendOtpService.execute(new SendOtpCommand(user, OneTimePassPurpose.LOGIN_VERIFICATION))) {
                case SendOtpResult.Failed failed -> new ManualLoginResult.Failed(failed.reason());
                case SendOtpResult.Success success -> new ManualLoginResult.OtpChallenge(
                        user.getId(),
                        user.getEmail(),
                        success.ttlSeconds()
                );
            };
        }

        TokenPair tokens = refreshTokenIssuer.startFamily(new StartTokenFamilyCommand(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                command.fingerprint()
        )).tokens();

        log.debug("Login success email={}", maskEmail(user.getEmail()));

        return new ManualLoginResult.Success(user.getId(), user.getEmail(), tokens);
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String maskEmail(String email) {
        if (email == null) {
            return "null";
        }
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
