package applications.features.otp.verification;

import applications.crypto.api.MatchingContract;
import applications.crypto.features.matching.MatchingCommand;
import applications.domain.entities.OneTimePassCode;
import applications.domain.repositories.OneTimePassCodeRepositories;
import applications.properties.OtpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyOtpService {

    private static final String GENERIC_REJECTION = "Invalid or expired code";

    private final OneTimePassCodeRepositories repositories;
    private final MatchingContract matchingContract;
    private final OtpProperties props;

    @Transactional
    public VerifyOtpResult execute(VerifyOtpCommand command) {
        if (command.userId() == null || command.code() == null || command.code().isBlank()) {
            return new VerifyOtpResult.Failed(GENERIC_REJECTION);
        }

        Optional<OneTimePassCode> active = repositories
                .findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                        command.userId(),
                        command.purpose()
                );

        if (active.isEmpty()) {
            return new VerifyOtpResult.Failed(GENERIC_REJECTION);
        }

        OneTimePassCode otp = active.get();

        if (otp.getAttempts() >= props.getMaxAttempts()) {
            otp.markConsumed();
            repositories.save(otp);
            return new VerifyOtpResult.Failed(GENERIC_REJECTION);
        }

        if (otp.isExpired(LocalDateTime.now())) {
            otp.markConsumed();
            repositories.save(otp);
            return new VerifyOtpResult.Failed(GENERIC_REJECTION);
        }

        if (!matchingContract.matches(new MatchingCommand(command.code(), otp.getCodeHash())).matched()) {
            otp.registerFailedAttempt();
            repositories.save(otp);
            log.debug("OTP mismatch user={} attempts={}", command.userId(), otp.getAttempts());
            return new VerifyOtpResult.Failed(GENERIC_REJECTION);
        }

        otp.markConsumed();
        repositories.save(otp);
        log.debug("OTP verified user={} purpose={}", command.userId(), command.purpose());

        return new VerifyOtpResult.Success(command.userId(), command.purpose());
    }
}
