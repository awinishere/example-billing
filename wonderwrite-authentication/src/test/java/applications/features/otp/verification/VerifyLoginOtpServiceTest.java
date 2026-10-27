package applications.features.otp.verification;

import applications.domain.entities.Credentials;
import applications.domain.entities.TestEntities;
import applications.domain.model.ClientFingerprint;
import applications.domain.repositories.CredentialsRepositories;
import applications.utils.tokens.RefreshTokenIssuer;
import applications.utils.tokens.StartTokenFamilyResult;
import applications.utils.tokens.TokenPair;
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
class VerifyLoginOtpServiceTest {

    @Mock
    private CredentialsRepositories credentialsRepositories;

    @Mock
    private VerifyOtpService verifyOtpService;

    @Mock
    private RefreshTokenIssuer refreshTokenIssuer;

    @InjectMocks
    private VerifyLoginOtpService service;

    private final UUID userId = UUID.randomUUID();
    private final ClientFingerprint fingerprint = new ClientFingerprint("agent", "203.0.113.7");

    @Test
    void rejectsUnknownUser() {
        when(credentialsRepositories.findById(userId)).thenReturn(Optional.empty());

        VerifyLoginOtpResult result = service.execute(
                new VerifyLoginOtpCommand(userId, "123456", fingerprint));

        assertThat(result).isInstanceOf(VerifyLoginOtpResult.Failed.class);
        assertThat(((VerifyLoginOtpResult.Failed) result).reason()).isEqualTo("Invalid or expired code");
        verify(refreshTokenIssuer, never()).startFamily(any());
    }

    @Test
    void propagatesOtpFailure() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "hash-value");
        when(credentialsRepositories.findById(userId)).thenReturn(Optional.of(user));
        when(verifyOtpService.execute(any())).thenReturn(new VerifyOtpResult.Failed("Invalid or expired code"));

        VerifyLoginOtpResult result = service.execute(
                new VerifyLoginOtpCommand(userId, "000000", fingerprint));

        assertThat(result).isInstanceOf(VerifyLoginOtpResult.Failed.class);
        verify(credentialsRepositories, never()).save(any());
        verify(refreshTokenIssuer, never()).startFamily(any());
    }

    @Test
    void verifiesEmailAndEstablishesSessionOnSuccess() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "hash-value");
        TokenPair pair = new TokenPair("access", "refresh", 900, 604800);
        when(credentialsRepositories.findById(userId)).thenReturn(Optional.of(user));
        when(verifyOtpService.execute(any()))
                .thenReturn(new VerifyOtpResult.Success(userId, applications.extensions.types.OneTimePassPurpose.LOGIN_VERIFICATION));
        when(refreshTokenIssuer.startFamily(any())).thenReturn(new StartTokenFamilyResult(pair));

        VerifyLoginOtpResult result = service.execute(
                new VerifyLoginOtpCommand(userId, "123456", fingerprint));

        assertThat(result).isInstanceOf(VerifyLoginOtpResult.Success.class);
        assertThat(((VerifyLoginOtpResult.Success) result).id()).isEqualTo(userId);
        assertThat(user.isEmailVerification()).isTrue();
        verify(credentialsRepositories).save(user);
    }
}
