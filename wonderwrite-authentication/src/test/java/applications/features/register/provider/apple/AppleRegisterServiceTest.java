package applications.features.register.provider.apple;

import applications.crypto.api.HashingContract;
import applications.crypto.features.hashing.HashingResult;
import applications.domain.entities.Credentials;
import applications.domain.entities.OAuthCredentials;
import applications.domain.entities.TestEntities;
import applications.domain.repositories.CredentialsRepositories;
import applications.domain.repositories.OAuthCredentialsRepositories;
import applications.extensions.types.RoleAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleRegisterServiceTest {

    private static final String SUBJECT = "001429.abc.def";

    @Mock
    private OAuthCredentialsRepositories oauthRepositories;

    @Mock
    private CredentialsRepositories credentialsRepositories;

    @Mock
    private HashingContract hashingContract;

    @InjectMocks
    private AppleRegisterService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void existingLinkReturnsAccountWithoutProvisioning() {
        Credentials account = TestEntities.credentials(userId, "user@icloud.com", "hash-value");
        when(oauthRepositories.findByProviderAndProviderUserId("apple", SUBJECT))
                .thenReturn(Optional.of(TestEntities.oauthLink(UUID.randomUUID(), userId, "apple", SUBJECT, "user@icloud.com")));
        when(credentialsRepositories.findById(userId)).thenReturn(Optional.of(account));

        AppleRegisterResult result = service.register(new AppleRegisterCommand(SUBJECT, "user@icloud.com"));

        assertThat(result).isInstanceOf(AppleRegisterResult.Success.class);
        assertThat(((AppleRegisterResult.Success) result).provisioned()).isFalse();
        verify(credentialsRepositories, never()).save(any());
        verify(oauthRepositories, never()).save(any());
    }

    @Test
    void linkPointingAtMissingAccountFails() {
        when(oauthRepositories.findByProviderAndProviderUserId("apple", SUBJECT))
                .thenReturn(Optional.of(TestEntities.oauthLink(UUID.randomUUID(), userId, "apple", SUBJECT, "user@icloud.com")));
        when(credentialsRepositories.findById(userId)).thenReturn(Optional.empty());

        AppleRegisterResult result = service.register(new AppleRegisterCommand(SUBJECT, "user@icloud.com"));

        assertThat(result).isInstanceOf(AppleRegisterResult.Failed.class);
        assertThat(((AppleRegisterResult.Failed) result).reason())
                .startsWith("OAuth link points at missing account");
    }

    @Test
    void unknownSubjectProvisionsAccountAndStoresLink() {
        when(oauthRepositories.findByProviderAndProviderUserId("apple", SUBJECT)).thenReturn(Optional.empty());
        when(hashingContract.hash(any())).thenReturn(new HashingResult("$argon2id$random"));
        when(credentialsRepositories.save(any(Credentials.class))).thenAnswer(invocation -> {
            Credentials entity = invocation.getArgument(0);
            return TestEntities.withId(entity, userId);
        });
        when(oauthRepositories.save(any(OAuthCredentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppleRegisterResult result = service.register(new AppleRegisterCommand(SUBJECT, "user@icloud.com"));

        ArgumentCaptor<OAuthCredentials> link = ArgumentCaptor.forClass(OAuthCredentials.class);
        verify(oauthRepositories).save(link.capture());
        assertThat(link.getValue().getProvider()).isEqualTo("apple");
        assertThat(link.getValue().getProviderUserId()).isEqualTo(SUBJECT);

        assertThat(result).isInstanceOf(AppleRegisterResult.Success.class);
        AppleRegisterResult.Success success = (AppleRegisterResult.Success) result;
        assertThat(success.id()).isEqualTo(userId);
        assertThat(success.role()).isEqualTo(RoleAccount.USER);
        assertThat(success.provisioned()).isTrue();
    }

    @Test
    void missingEmailResolvesToSyntheticDomain() {
        when(oauthRepositories.findByProviderAndProviderUserId("apple", SUBJECT)).thenReturn(Optional.empty());
        when(hashingContract.hash(any())).thenReturn(new HashingResult("$argon2id$random"));
        when(credentialsRepositories.save(any(Credentials.class))).thenAnswer(invocation -> {
            Credentials entity = invocation.getArgument(0);
            return TestEntities.withId(entity, userId);
        });
        when(oauthRepositories.save(any(OAuthCredentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(new AppleRegisterCommand(SUBJECT, null));

        ArgumentCaptor<Credentials> saved = ArgumentCaptor.forClass(Credentials.class);
        verify(credentialsRepositories).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("apple." + SUBJECT + "@oauth.invalid");
    }

    @Test
    void emailCollisionLinksProviderToExistingAccount() {
        UUID existingId = UUID.randomUUID();
        Credentials existing = TestEntities.credentials(existingId, "user@icloud.com", "hash-value");
        when(oauthRepositories.findByProviderAndProviderUserId("apple", SUBJECT)).thenReturn(Optional.empty());
        when(hashingContract.hash(any())).thenReturn(new HashingResult("$argon2id$random"));
        when(credentialsRepositories.save(any(Credentials.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));
        when(credentialsRepositories.findByEmail("user@icloud.com")).thenReturn(Optional.of(existing));
        when(oauthRepositories.save(any(OAuthCredentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppleRegisterResult result = service.register(new AppleRegisterCommand(SUBJECT, "user@icloud.com"));

        assertThat(result).isInstanceOf(AppleRegisterResult.Success.class);
        assertThat(((AppleRegisterResult.Success) result).id()).isEqualTo(existingId);
        ArgumentCaptor<OAuthCredentials> link = ArgumentCaptor.forClass(OAuthCredentials.class);
        verify(oauthRepositories).save(link.capture());
        assertThat(link.getValue().getUserId()).isEqualTo(existingId);
    }

    @Test
    void concurrentLinkRaceResolvesToWinner() {
        Credentials winner = TestEntities.credentials(userId, "user@icloud.com", "hash-value");
        when(oauthRepositories.findByProviderAndProviderUserId("apple", SUBJECT))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(TestEntities.oauthLink(UUID.randomUUID(), userId, "apple", SUBJECT, "user@icloud.com")));
        when(hashingContract.hash(any())).thenReturn(new HashingResult("$argon2id$random"));
        when(credentialsRepositories.save(any(Credentials.class))).thenAnswer(invocation -> {
            Credentials entity = invocation.getArgument(0);
            return TestEntities.withId(entity, UUID.randomUUID());
        });
        when(oauthRepositories.save(any(OAuthCredentials.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate subject"));
        when(credentialsRepositories.findById(userId)).thenReturn(Optional.of(winner));

        AppleRegisterResult result = service.register(new AppleRegisterCommand(SUBJECT, "user@icloud.com"));

        assertThat(result).isInstanceOf(AppleRegisterResult.Success.class);
        assertThat(((AppleRegisterResult.Success) result).provisioned()).isFalse();
    }
}
