package applications.features.otp.verification;

import applications.crypto.api.MatchingContract;
import applications.crypto.features.matching.MatchingResult;
import applications.domain.entities.OneTimePassCode;
import applications.domain.entities.TestEntities;
import applications.domain.repositories.OneTimePassCodeRepositories;
import applications.extensions.types.OneTimePassPurpose;
import applications.properties.OtpProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyOtpServiceTest {

    private static final String GENERIC = "Invalid or expired code";

    @Mock
    private OneTimePassCodeRepositories repositories;

    @Mock
    private MatchingContract matchingContract;

    @Mock
    private OtpProperties props;

    @InjectMocks
    private VerifyOtpService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void rejectsBlankCodeWithoutTouchingRepository() {
        VerifyOtpResult result = service.execute(
                new VerifyOtpCommand(userId, OneTimePassPurpose.LOGIN_VERIFICATION, " "));

        assertThat(result).isInstanceOf(VerifyOtpResult.Failed.class);
        assertThat(((VerifyOtpResult.Failed) result).reason()).isEqualTo(GENERIC);
        verify(repositories, never()).findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void rejectsNullUserId() {
        VerifyOtpResult result = service.execute(
                new VerifyOtpCommand(null, OneTimePassPurpose.LOGIN_VERIFICATION, "123456"));

        assertThat(result).isInstanceOf(VerifyOtpResult.Failed.class);
    }

    @Test
    void rejectsWhenNoActiveCode() {
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.LOGIN_VERIFICATION)).thenReturn(Optional.empty());

        VerifyOtpResult result = service.execute(
                new VerifyOtpCommand(userId, OneTimePassPurpose.LOGIN_VERIFICATION, "123456"));

        assertThat(result).isInstanceOf(VerifyOtpResult.Failed.class);
    }

    @Test
    void consumesCodeWhenMaxAttemptsExhausted() {
        OneTimePassCode otp = TestEntities.otpCode(UUID.randomUUID(), userId,
                OneTimePassPurpose.LOGIN_VERIFICATION, "hash", LocalDateTime.now().plusMinutes(5), LocalDateTime.now());
        otp.registerFailedAttempt();
        otp.registerFailedAttempt();
        otp.registerFailedAttempt();
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.LOGIN_VERIFICATION)).thenReturn(Optional.of(otp));
        when(props.getMaxAttempts()).thenReturn(3);

        VerifyOtpResult result = service.execute(
                new VerifyOtpCommand(userId, OneTimePassPurpose.LOGIN_VERIFICATION, "123456"));

        assertThat(result).isInstanceOf(VerifyOtpResult.Failed.class);
        assertThat(otp.isConsumed()).isTrue();
        verify(repositories).save(otp);
        verify(matchingContract, never()).matches(any());
    }

    @Test
    void consumesExpiredCode() {
        OneTimePassCode otp = TestEntities.otpCode(UUID.randomUUID(), userId,
                OneTimePassPurpose.LOGIN_VERIFICATION, "hash", LocalDateTime.now().minusSeconds(1), LocalDateTime.now());
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.LOGIN_VERIFICATION)).thenReturn(Optional.of(otp));
        when(props.getMaxAttempts()).thenReturn(3);

        VerifyOtpResult result = service.execute(
                new VerifyOtpCommand(userId, OneTimePassPurpose.LOGIN_VERIFICATION, "123456"));

        assertThat(result).isInstanceOf(VerifyOtpResult.Failed.class);
        assertThat(otp.isConsumed()).isTrue();
    }

    @Test
    void registersFailedAttemptOnMismatch() {
        OneTimePassCode otp = TestEntities.otpCode(UUID.randomUUID(), userId,
                OneTimePassPurpose.LOGIN_VERIFICATION, "$argon2id$hash", LocalDateTime.now().plusMinutes(5), LocalDateTime.now());
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.LOGIN_VERIFICATION)).thenReturn(Optional.of(otp));
        when(props.getMaxAttempts()).thenReturn(3);
        when(matchingContract.matches(any())).thenReturn(new MatchingResult(false));

        VerifyOtpResult result = service.execute(
                new VerifyOtpCommand(userId, OneTimePassPurpose.LOGIN_VERIFICATION, "000000"));

        assertThat(result).isInstanceOf(VerifyOtpResult.Failed.class);
        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(otp.isConsumed()).isFalse();
    }

    @Test
    void consumesAndSucceedsOnMatch() {
        OneTimePassCode otp = TestEntities.otpCode(UUID.randomUUID(), userId,
                OneTimePassPurpose.LOGIN_VERIFICATION, "$argon2id$hash", LocalDateTime.now().plusMinutes(5), LocalDateTime.now());
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.LOGIN_VERIFICATION)).thenReturn(Optional.of(otp));
        when(props.getMaxAttempts()).thenReturn(3);
        when(matchingContract.matches(any())).thenReturn(new MatchingResult(true));

        VerifyOtpResult result = service.execute(
                new VerifyOtpCommand(userId, OneTimePassPurpose.LOGIN_VERIFICATION, "123456"));

        assertThat(result).isInstanceOf(VerifyOtpResult.Success.class);
        assertThat(((VerifyOtpResult.Success) result).userId()).isEqualTo(userId);
        assertThat(otp.isConsumed()).isTrue();
    }
}
