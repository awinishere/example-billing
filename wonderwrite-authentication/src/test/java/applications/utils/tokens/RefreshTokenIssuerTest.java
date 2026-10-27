package applications.utils.tokens;

import applications.domain.entities.RefreshSecret;
import applications.domain.model.ClientFingerprint;
import applications.domain.repositories.RefreshSecretRepositories;
import applications.extensions.types.RoleAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenIssuerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshSecretRepositories repositories;

    @InjectMocks
    private RefreshTokenIssuer issuer;

    @Test
    void startFamilyStoresHashedSecretAndReturnsPair() {
        UUID userId = UUID.randomUUID();
        TokenPair pair = new TokenPair("access-value", "refresh-value", 900, 604800);
        when(jwtTokenProvider.issue(any())).thenReturn(new IssueTokenResult(pair));

        StartTokenFamilyResult result = issuer.startFamily(new StartTokenFamilyCommand(
                userId, "user@example.com", RoleAccount.USER, new ClientFingerprint("Mozilla/5.0", "10.0.0.1")));

        assertThat(result.tokens()).isEqualTo(pair);

        ArgumentCaptor<IssueTokenCommand> issued = ArgumentCaptor.forClass(IssueTokenCommand.class);
        verify(jwtTokenProvider).issue(issued.capture());
        assertThat(issued.getValue().userId()).isEqualTo(userId);
        assertThat(issued.getValue().email()).isEqualTo("user@example.com");
        assertThat(issued.getValue().role()).isEqualTo(RoleAccount.USER);

        ArgumentCaptor<RefreshSecret> saved = ArgumentCaptor.forClass(RefreshSecret.class);
        verify(repositories).save(saved.capture());
        RefreshSecret secret = saved.getValue();
        assertThat(secret.getUserId()).isEqualTo(userId);
        assertThat(secret.getFamilyId()).isNotNull();
        assertThat(secret.getSecretHash()).isEqualTo(sha256Hex("refresh-value"));
        assertThat(secret.getSecretHash()).isNotEqualTo("refresh-value");
        assertThat(secret.isRevoked()).isFalse();
        assertThat(secret.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(secret.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(secret.getExpiresAt()).isAfter(LocalDateTime.now().plus(Duration.ofDays(7)).minusMinutes(5));
    }

    @Test
    void startFamilyWithoutFingerprintStoresUnknownClient() {
        UUID userId = UUID.randomUUID();
        when(jwtTokenProvider.issue(any()))
                .thenReturn(new IssueTokenResult(new TokenPair("access-value", "refresh-value", 900, 604800)));

        issuer.startFamily(new StartTokenFamilyCommand(userId, "user@example.com", RoleAccount.USER, null));

        ArgumentCaptor<RefreshSecret> saved = ArgumentCaptor.forClass(RefreshSecret.class);
        verify(repositories).save(saved.capture());
        assertThat(saved.getValue().getUserAgent()).isNull();
        assertThat(saved.getValue().getIpAddress()).isNull();
    }

    @Test
    void revokeAllSessionsRevokesEveryActiveSecretAndReturnsCount() {
        UUID userId = UUID.randomUUID();
        RefreshSecret first = activeSecret(userId);
        RefreshSecret second = activeSecret(userId);
        when(repositories.findByUserIdAndRevokedFalse(userId)).thenReturn(List.of(first, second));

        int revoked = issuer.revokeAllSessions(userId);

        assertThat(revoked).isEqualTo(2);
        assertThat(first.isRevoked()).isTrue();
        assertThat(second.isRevoked()).isTrue();
        verify(repositories).saveAll(List.of(first, second));
    }

    @Test
    void revokeAllSessionsWithoutActiveSecretsReturnsZero() {
        UUID userId = UUID.randomUUID();
        when(repositories.findByUserIdAndRevokedFalse(userId)).thenReturn(List.of());

        int revoked = issuer.revokeAllSessions(userId);

        assertThat(revoked).isZero();
        verify(repositories).saveAll(List.of());
    }

    private RefreshSecret activeSecret(UUID userId) {
        return RefreshSecret.newSecret(userId, UUID.randomUUID(), "hash-value",
                LocalDateTime.now().plusDays(7), new ClientFingerprint("agent", "127.0.0.1"));
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
