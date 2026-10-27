package applications.features.login.provider.google;

import applications.domain.entities.Credentials;
import applications.domain.entities.TestEntities;
import applications.domain.model.ClientFingerprint;
import applications.domain.repositories.CredentialsRepositories;
import applications.domain.repositories.OAuthCredentialsRepositories;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleLoginServiceTest {

    private static final String SUBJECT = "110483738369414381905";

    @Mock
    private OAuthCredentialsRepositories oAuthCredentialsRepositories;

    @Mock
    private CredentialsRepositories credentialsRepositories;

    @Mock
    private RefreshTokenIssuer refreshTokenIssuer;

    @InjectMocks
    private GoogleLoginService service;

    private final UUID userId = UUID.randomUUID();
    private final ClientFingerprint fingerprint = new ClientFingerprint("agent", "203.0.113.7");

    @Test
    void unknownIdentityIsRejectedWithoutProvisioning() {
        when(oAuthCredentialsRepositories.findByProviderAndProviderUserId("google", SUBJECT))
                .thenReturn(Optional.empty());

        GoogleLoginResult result = service.login(new GoogleLoginCommand(SUBJECT, "user@gmail.com", fingerprint));

        assertThat(result).isInstanceOf(GoogleLoginResult.Failed.class);
        assertThat(((GoogleLoginResult.Failed) result).reason()).isEqualTo("Unknown Google identity");
        verify(refreshTokenIssuer, never()).startFamily(any());
    }

    @Test
    void linkPointingAtMissingAccountFails() {
        when(oAuthCredentialsRepositories.findByProviderAndProviderUserId("google", SUBJECT))
                .thenReturn(Optional.of(TestEntities.oauthLink(UUID.randomUUID(), userId, "google", SUBJECT, "user@gmail.com")));
        when(credentialsRepositories.findById(userId)).thenReturn(Optional.empty());

        GoogleLoginResult result = service.login(new GoogleLoginCommand(SUBJECT, "user@gmail.com", fingerprint));

        assertThat(result).isInstanceOf(GoogleLoginResult.Failed.class);
        assertThat(((GoogleLoginResult.Failed) result).reason())
                .isEqualTo("OAuth link points at missing account");
        verify(refreshTokenIssuer, never()).startFamily(any());
    }

    @Test
    void knownIdentityEstablishesSessionWithoutProvisioning() {
        Credentials account = TestEntities.credentials(userId, "user@gmail.com", "$argon2id$stored");
        TokenPair pair = new TokenPair("access", "refresh", 900, 604800);
        when(oAuthCredentialsRepositories.findByProviderAndProviderUserId("google", SUBJECT))
                .thenReturn(Optional.of(TestEntities.oauthLink(UUID.randomUUID(), userId, "google", SUBJECT, "user@gmail.com")));
        when(credentialsRepositories.findById(userId)).thenReturn(Optional.of(account));
        when(refreshTokenIssuer.startFamily(any())).thenReturn(new StartTokenFamilyResult(pair));

        GoogleLoginResult result = service.login(new GoogleLoginCommand(SUBJECT, "user@gmail.com", fingerprint));

        ArgumentCaptor<StartTokenFamilyCommand> family = ArgumentCaptor.forClass(StartTokenFamilyCommand.class);
        verify(refreshTokenIssuer).startFamily(family.capture());
        assertThat(family.getValue().userId()).isEqualTo(userId);
        assertThat(family.getValue().email()).isEqualTo("user@gmail.com");
        assertThat(family.getValue().fingerprint()).isEqualTo(fingerprint);

        assertThat(result).isInstanceOf(GoogleLoginResult.Success.class);
        GoogleLoginResult.Success success = (GoogleLoginResult.Success) result;
        assertThat(success.id()).isEqualTo(userId);
        assertThat(success.tokens()).isEqualTo(pair);
        assertThat(success.provisioned()).isFalse();
    }
}
