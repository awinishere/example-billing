package applications.features.otp.send;

import applications.crypto.api.HashingContract;
import applications.crypto.features.hashing.HashingCommand;
import applications.crypto.features.hashing.HashingResult;
import applications.domain.entities.Credentials;
import applications.domain.entities.OneTimePassCode;
import applications.domain.entities.TestEntities;
import applications.domain.repositories.OneTimePassCodeRepositories;
import applications.emails.api.SendEmailContract;
import applications.emails.features.send.SendEmailCommand;
import applications.extensions.types.OneTimePassPurpose;
import applications.properties.OtpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendOtpServiceTest {

    @Mock
    private OneTimePassCodeRepositories repositories;

    @Mock
    private HashingContract hashingContract;

    @Mock
    private SendEmailContract sendEmailContract;

    @Mock
    private OtpProperties props;

    @InjectMocks
    private SendOtpService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void wireProductName() {
        ReflectionTestUtils.setField(service, "productName", "Wonderwrite");
    }

    private void stubGeneration() {
        when(props.getCodeLength()).thenReturn(6);
        when(props.getTtl()).thenReturn(Duration.ofMinutes(5));
    }

    @Test
    void dispatchesNewOtpWhenNoneActive() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "hash-value");
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.LOGIN_VERIFICATION)).thenReturn(Optional.empty());
        stubGeneration();
        when(hashingContract.hash(any())).thenReturn(new HashingResult("$argon2id$otphash"));

        SendOtpResult result = service.execute(new SendOtpCommand(user, OneTimePassPurpose.LOGIN_VERIFICATION));

        verify(repositories).save(any(OneTimePassCode.class));
        ArgumentCaptor<SendEmailCommand> mail = ArgumentCaptor.forClass(SendEmailCommand.class);
        verify(sendEmailContract).execute(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo("user@example.com");
        assertThat(mail.getValue().template()).isEqualTo("otp-code");
        assertThat(mail.getValue().subject()).isEqualTo("Your Wonderwrite verification code");
        assertThat(mail.getValue().model()).containsKeys("productName", "recipientName", "code", "ttlMinutes");
        assertThat(mail.getValue().model().get("recipientName")).isEqualTo("user");

        assertThat(result).isInstanceOf(SendOtpResult.Success.class);
        assertThat(((SendOtpResult.Success) result).ttlSeconds()).isEqualTo(300);
    }

    @Test
    void blocksResendWithinGap() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "hash-value");
        OneTimePassCode fresh = TestEntities.otpCode(UUID.randomUUID(), userId,
                OneTimePassPurpose.LOGIN_VERIFICATION, "hash", LocalDateTime.now().plusMinutes(5), LocalDateTime.now());
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.LOGIN_VERIFICATION)).thenReturn(Optional.of(fresh));
        when(props.getResendGap()).thenReturn(Duration.ofMinutes(1));

        SendOtpResult result = service.execute(new SendOtpCommand(user, OneTimePassPurpose.LOGIN_VERIFICATION));

        assertThat(result).isInstanceOf(SendOtpResult.Failed.class);
        verify(repositories, never()).save(any());
        verify(sendEmailContract, never()).execute(any());
    }

    @Test
    void allowsResendAfterGapElapsed() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "hash-value");
        OneTimePassCode stale = TestEntities.otpCode(UUID.randomUUID(), userId,
                OneTimePassPurpose.PASSWORD_RESET, "hash", LocalDateTime.now().minusMinutes(4),
                LocalDateTime.now().minusMinutes(3));
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.PASSWORD_RESET)).thenReturn(Optional.of(stale));
        when(props.getResendGap()).thenReturn(Duration.ofMinutes(1));
        stubGeneration();
        when(hashingContract.hash(any())).thenReturn(new HashingResult("$argon2id$otphash"));

        SendOtpResult result = service.execute(new SendOtpCommand(user, OneTimePassPurpose.PASSWORD_RESET));

        assertThat(result).isInstanceOf(SendOtpResult.Success.class);
        verify(repositories).save(any(OneTimePassCode.class));
    }

    @Test
    void codeHashIsPersistedNotPlainText() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "hash-value");
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                any(), any())).thenReturn(Optional.empty());
        stubGeneration();
        when(hashingContract.hash(any())).thenReturn(new HashingResult("$argon2id$otphash"));

        service.execute(new SendOtpCommand(user, OneTimePassPurpose.LOGIN_VERIFICATION));

        ArgumentCaptor<OneTimePassCode> saved = ArgumentCaptor.forClass(OneTimePassCode.class);
        verify(repositories).save(saved.capture());
        assertThat(saved.getValue().getCodeHash()).isEqualTo("$argon2id$otphash");
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    void hashingReceivesGeneratedNumericCode() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "hash-value");
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                any(), any())).thenReturn(Optional.empty());
        stubGeneration();
        when(hashingContract.hash(any())).thenReturn(new HashingResult("$argon2id$otphash"));

        service.execute(new SendOtpCommand(user, OneTimePassPurpose.LOGIN_VERIFICATION));

        ArgumentCaptor<HashingCommand> hashed = ArgumentCaptor.forClass(HashingCommand.class);
        verify(hashingContract).hash(hashed.capture());
        assertThat(hashed.getValue().plainText()).matches("\\d{6}");
    }

    @Test
    void localPartFallsBackToWholeEmailWhenNoAtSign() {
        Credentials user = TestEntities.credentials(userId, "weirduser", "hash-value");
        when(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                any(), any())).thenReturn(Optional.empty());
        stubGeneration();
        when(hashingContract.hash(any())).thenReturn(new HashingResult("$argon2id$otphash"));

        service.execute(new SendOtpCommand(user, OneTimePassPurpose.LOGIN_VERIFICATION));

        ArgumentCaptor<SendEmailCommand> mail = ArgumentCaptor.forClass(SendEmailCommand.class);
        verify(sendEmailContract).execute(mail.capture());
        assertThat(mail.getValue().model().get("recipientName")).isEqualTo("weirduser");
    }
}
