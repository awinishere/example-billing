package applications.features.login.provider.manual;

import applications.crypto.api.MatchingContract;
import applications.crypto.features.matching.MatchingCommand;
import applications.crypto.features.matching.MatchingResult;
import applications.domain.entities.Credentials;
import applications.domain.entities.TestEntities;
import applications.domain.model.ClientFingerprint;
import applications.domain.repositories.CredentialsRepositories;
import applications.features.login.guard.AssertLoginAllowedCommand;
import applications.features.login.guard.LoginAttemptTracker;
import applications.features.login.guard.RecordLoginAttemptCommand;
import applications.features.otp.send.SendOtpCommand;
import applications.features.otp.send.SendOtpResult;
import applications.features.otp.send.SendOtpService;
import applications.extensions.exception.RateLimitedException;
import applications.extensions.types.OneTimePassPurpose;
import applications.extensions.types.RoleAccount;
import applications.utils.tokens.RefreshTokenIssuer;
import applications.utils.tokens.StartTokenFamilyCommand;
import applications.utils.tokens.StartTokenFamilyResult;
import applications.utils.tokens.TokenPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualLoginServiceTest {

    @Mock
    private CredentialsRepositories credentialsRepositories;

    @Mock
    private MatchingContract matchingContract;

    @Mock
    private SendOtpService sendOtpService;

    @Mock
    private RefreshTokenIssuer refreshTokenIssuer;

    @Mock
    private LoginAttemptTracker attemptTracker;

    @InjectMocks
    private ManualLoginService service;

    private final UUID userId = UUID.randomUUID();
    private final ClientFingerprint fingerprint = new ClientFingerprint("agent", "203.0.113.7");

    @Test
    void unknownEmailIsRejectedWithoutPasswordCheck() {
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.empty());

        ManualLoginResult result = service.execute(
                new ManualLoginCommand("user@example.com", "any-password", fingerprint));

        assertThat(result).isInstanceOf(ManualLoginResult.Failed.class);
        assertThat(((ManualLoginResult.Failed) result).reason()).isEqualTo("Invalid email or password");
        verify(matchingContract, never()).matches(any());
        verify(attemptTracker).record(new RecordLoginAttemptCommand("user@example.com", "203.0.113.7", false));
    }

    @Test
    void wrongPasswordIsRejected() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "$argon2id$stored");
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(matchingContract.matches(new MatchingCommand("bad-password", "$argon2id$stored")))
                .thenReturn(new MatchingResult(false));

        ManualLoginResult result = service.execute(
                new ManualLoginCommand("user@example.com", "bad-password", fingerprint));

        assertThat(result).isInstanceOf(ManualLoginResult.Failed.class);
        verify(attemptTracker).record(new RecordLoginAttemptCommand("user@example.com", "203.0.113.7", false));
        verify(refreshTokenIssuer, never()).startFamily(any());
    }

    @Test
    void emailIsNormalizedBeforeLookup() {
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.empty());

        service.execute(new ManualLoginCommand("  User@Example.COM ", "any-password", fingerprint));

        verify(attemptTracker).assertAllowed(new AssertLoginAllowedCommand("user@example.com", "203.0.113.7"));
    }

    @Test
    void unverifiedEmailReceivesOtpChallenge() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "$argon2id$stored");
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(matchingContract.matches(any())).thenReturn(new MatchingResult(true));
        when(sendOtpService.execute(new SendOtpCommand(user, OneTimePassPurpose.LOGIN_VERIFICATION)))
                .thenReturn(new SendOtpResult.Success("user@example.com", OneTimePassPurpose.LOGIN_VERIFICATION, 300));

        ManualLoginResult result = service.execute(
                new ManualLoginCommand("user@example.com", "right-password", fingerprint));

        assertThat(result).isInstanceOf(ManualLoginResult.OtpChallenge.class);
        ManualLoginResult.OtpChallenge challenge = (ManualLoginResult.OtpChallenge) result;
        assertThat(challenge.userId()).isEqualTo(userId);
        assertThat(challenge.ttlSeconds()).isEqualTo(300);
        verify(refreshTokenIssuer, never()).startFamily(any());
        verify(attemptTracker).record(new RecordLoginAttemptCommand("user@example.com", "203.0.113.7", true));
    }

    @Test
    void failedOtpDispatchBecomesLoginFailure() {
        Credentials user = TestEntities.credentials(userId, "user@example.com", "$argon2id$stored");
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(matchingContract.matches(any())).thenReturn(new MatchingResult(true));
        when(sendOtpService.execute(any()))
                .thenReturn(new SendOtpResult.Failed("An OTP is already active; try again shortly"));

        ManualLoginResult result = service.execute(
                new ManualLoginCommand("user@example.com", "right-password", fingerprint));

        assertThat(result).isInstanceOf(ManualLoginResult.Failed.class);
        assertThat(((ManualLoginResult.Failed) result).reason())
                .isEqualTo("An OTP is already active; try again shortly");
    }

    @Test
    void verifiedEmailEstablishesSession() {
        Credentials user = TestEntities.verifiedCredentials(userId, "user@example.com", "$argon2id$stored");
        TokenPair pair = new TokenPair("access", "refresh", 900, 604800);
        when(credentialsRepositories.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(matchingContract.matches(any())).thenReturn(new MatchingResult(true));
        when(refreshTokenIssuer.startFamily(any())).thenReturn(new StartTokenFamilyResult(pair));

        ManualLoginResult result = service.execute(
                new ManualLoginCommand("user@example.com", "right-password", fingerprint));

        ArgumentCaptor<StartTokenFamilyCommand> family = ArgumentCaptor.forClass(StartTokenFamilyCommand.class);
        verify(refreshTokenIssuer).startFamily(family.capture());
        assertThat(family.getValue().userId()).isEqualTo(userId);
        assertThat(family.getValue().role()).isEqualTo(RoleAccount.USER);
        assertThat(family.getValue().fingerprint()).isEqualTo(fingerprint);

        assertThat(result).isInstanceOf(ManualLoginResult.Success.class);
        ManualLoginResult.Success success = (ManualLoginResult.Success) result;
        assertThat(success.id()).isEqualTo(userId);
        assertThat(success.tokens()).isEqualTo(pair);
    }

    @Test
    void rateLimitExceptionPropagates() {
        RateLimitedException limited = new RateLimitedException("Too many failed login attempts", Duration.ofMinutes(15));
        doThrow(limited).when(attemptTracker).assertAllowed(any());

        assertThatThrownBy(() -> service.execute(
                new ManualLoginCommand("user@example.com", "any-password", fingerprint)))
                .isSameAs(limited);
    }
}
