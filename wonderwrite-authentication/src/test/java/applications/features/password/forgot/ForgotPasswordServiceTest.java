package applications.features.password.forgot;

import applications.domain.entities.Credentials;
import applications.domain.entities.TestEntities;
import applications.domain.repositories.CredentialsRepositories;
import applications.features.otp.send.SendOtpCommand;
import applications.features.otp.send.SendOtpResult;
import applications.features.otp.send.SendOtpService;
import applications.extensions.types.OneTimePassPurpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {

    @Mock
    private CredentialsRepositories credentialsRepositories;

    @Mock
    private SendOtpService sendOtpService;

    @InjectMocks
    private ForgotPasswordService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void unknownEmailStillSucceedsButDispatchesNothing() {
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.empty());

        ForgotPasswordResult result = service.execute(new ForgotPasswordCommand("user@example.com"));

        assertThat(result).isInstanceOf(ForgotPasswordResult.Success.class);
        assertThat(((ForgotPasswordResult.Success) result).dispatched()).isFalse();
        verify(sendOtpService, never()).execute(any());
    }

    @Test
    void knownEmailDispatchesResetOtp() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "hash-value");
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(sendOtpService.execute(new SendOtpCommand(user, OneTimePassPurpose.PASSWORD_RESET)))
                .thenReturn(new SendOtpResult.Success("user@example.com", OneTimePassPurpose.PASSWORD_RESET, 300));

        ForgotPasswordResult result = service.execute(new ForgotPasswordCommand("user@example.com"));

        assertThat(result).isInstanceOf(ForgotPasswordResult.Success.class);
        assertThat(((ForgotPasswordResult.Success) result).dispatched()).isTrue();
        assertThat(((ForgotPasswordResult.Success) result).ttlSeconds()).isEqualTo(300);
    }

    @Test
    void resendBlockedStillReportsSuccess() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "hash-value");
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(sendOtpService.execute(any()))
                .thenReturn(new SendOtpResult.Failed("An OTP is already active; try again shortly"));

        ForgotPasswordResult result = service.execute(new ForgotPasswordCommand("user@example.com"));

        assertThat(result).isInstanceOf(ForgotPasswordResult.Success.class);
        assertThat(((ForgotPasswordResult.Success) result).dispatched()).isFalse();
    }

    @Test
    void emailIsNormalizedBeforeLookup() {
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.empty());

        service.execute(new ForgotPasswordCommand("  User@Example.COM "));

        verify(credentialsRepositories).findByEmail("user@example.com");
    }
}
