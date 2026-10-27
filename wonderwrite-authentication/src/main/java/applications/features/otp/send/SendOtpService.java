package applications.features.otp.send;

import applications.crypto.api.HashingContract;
import applications.crypto.features.hashing.HashingCommand;
import applications.domain.entities.Credentials;
import applications.domain.entities.OneTimePassCode;
import applications.domain.repositories.OneTimePassCodeRepositories;
import applications.emails.api.SendEmailContract;
import applications.emails.features.send.SendEmailCommand;
import applications.extensions.types.OneTimePassPurpose;
import applications.properties.OtpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendOtpService {

    private static final int DIGIT_BOUND = 10;

    private final OneTimePassCodeRepositories repositories;
    private final HashingContract hashingContract;
    private final SendEmailContract sendEmailContract;
    private final OtpProperties props;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.product-name}")
    private String productName;

    @Transactional
    public SendOtpResult execute(SendOtpCommand command) {
        Credentials user = command.credentials();
        OneTimePassPurpose purpose = command.purpose();

        Optional<OneTimePassCode> active = repositories
                .findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(user.getId(), purpose);

        if (active.isPresent()) {
            LocalDateTime earliestResend = active.get().getCreatedAt().plus(props.getResendGap());
            if (LocalDateTime.now().isBefore(earliestResend)) {
                return new SendOtpResult.Failed("An OTP is already active; try again shortly");
            }
        }

        String code = nextCode();
        repositories.save(OneTimePassCode.newCode(
                user.getId(),
                purpose,
                hashingContract.hash(new HashingCommand(code)).hash(),
                LocalDateTime.now().plus(props.getTtl())
        ));

        sendEmailContract.execute(new SendEmailCommand(
                null,
                user.getEmail(),
                null,
                "Your %s verification code".formatted(productName),
                "otp-code",
                Map.of(
                        "productName", productName,
                        "recipientName", localPart(user.getEmail()),
                        "code", code,
                        "ttlMinutes", props.getTtl().toMinutes()
                )
        ));

        if (log.isDebugEnabled()) {
            log.debug("OTP dispatched user={} purpose={} ttl={}", user.getId(), purpose, props.getTtl());
        }

        return new SendOtpResult.Success(user.getEmail(), purpose, (int) props.getTtl().toSeconds());
    }

    private String nextCode() {
        StringBuilder sb = new StringBuilder(props.getCodeLength());
        for (int i = 0; i < props.getCodeLength(); i++) {
            sb.append(secureRandom.nextInt(DIGIT_BOUND));
        }
        return sb.toString();
    }

    private String localPart(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
