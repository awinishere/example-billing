package applications.features.password.reset;

import applications.crypto.api.HashingContract;
import applications.crypto.features.hashing.HashingCommand;
import applications.crypto.features.hashing.HashingResult;
import applications.domain.entities.Credentials;
import applications.domain.entities.TestEntities;
import applications.domain.repositories.CredentialsRepositories;
import applications.features.otp.verification.VerifyOtpResult;
import applications.features.otp.verification.VerifyOtpService;
import applications.utils.tokens.RefreshTokenIssuer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ResetPasswordServiceTest {

    private static final String GENERIC = "Invalid or expired reset code";

    @Mock
    private CredentialsRepositories credentialsRepositories;

    @Mock
    private VerifyOtpService verifyOtpService;

    @Mock
    private HashingContract hashingContract;

    @Mock
    private RefreshTokenIssuer refreshTokenIssuer;

    @InjectMocks
    private ResetPasswordService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void unknownEmailIsRejected() {
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.empty());

        ResetPasswordResult result = service.execute(
                new ResetPasswordCommand("user@example.com", "123456", "NewPass!234"));

        assertThat(result).isInstanceOf(ResetPasswordResult.Failed.class);
        assertThat(((ResetPasswordResult.Failed) result).reason()).isEqualTo(GENERIC);
    }

    @Test
    void invalidOtpIsRejected() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "old-hash");
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(verifyOtpService.execute(any())).thenReturn(new VerifyOtpResult.Failed("Invalid or expired code"));

        ResetPasswordResult result = service.execute(
                new ResetPasswordCommand("user@example.com", "000000", "NewPass!234"));

        assertThat(result).isInstanceOf(ResetPasswordResult.Failed.class);
        verify(credentialsRepositories, never()).save(any());
        verify(refreshTokenIssuer, never()).revokeAllSessions(any());
    }

    @Test
    void validOtpResetsPasswordAndRevokesSessions() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "old-hash");
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(verifyOtpService.execute(any()))
                .thenReturn(new VerifyOtpResult.Success(userId, applications.extensions.types.OneTimePassPurpose.PASSWORD_RESET));
        when(hashingContract.hash(new HashingCommand("NewPass!234"))).thenReturn(new HashingResult("$argon2id$new"));
        when(refreshTokenIssuer.revokeAllSessions(userId)).thenReturn(3);

        ResetPasswordResult result = service.execute(
                new ResetPasswordCommand("user@example.com", "123456", "NewPass!234"));

        assertThat(result).isInstanceOf(ResetPasswordResult.Success.class);
        assertThat(((ResetPasswordResult.Success) result).id()).isEqualTo(userId);
        assertThat(((ResetPasswordResult.Success) result).sessionsRevoked()).isEqualTo(3);
        assertThat(user.getPassword()).isEqualTo("$argon2id$new");

        ArgumentCaptor<Credentials> saved = ArgumentCaptor.forClass(Credentials.class);
        verify(credentialsRepositories).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("$argon2id$new");
    }
}
