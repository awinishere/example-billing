package applications.domain.entities;

import applications.domain.model.ClientFingerprint;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshSecretTest {

    @Test
    void newSecretStartsActiveWithClientInfo() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        RefreshSecret secret = RefreshSecret.newSecret(userId, familyId, "hash-value",
                expiresAt, new ClientFingerprint("Mozilla/5.0", "10.0.0.1"));

        assertThat(secret.getId()).isNull();
        assertThat(secret.getUserId()).isEqualTo(userId);
        assertThat(secret.getFamilyId()).isEqualTo(familyId);
        assertThat(secret.getSecretHash()).isEqualTo("hash-value");
        assertThat(secret.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(secret.isRevoked()).isFalse();
        assertThat(secret.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(secret.getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    void nullFingerprintFallsBackToUnknownClient() {
        RefreshSecret secret = RefreshSecret.newSecret(UUID.randomUUID(), UUID.randomUUID(),
                "hash-value", LocalDateTime.now().plusDays(1), null);

        assertThat(secret.getUserAgent()).isNull();
        assertThat(secret.getIpAddress()).isNull();
    }

    @Test
    void revokeMarksSecretRevoked() {
        RefreshSecret secret = RefreshSecret.newSecret(UUID.randomUUID(), UUID.randomUUID(),
                "hash-value", LocalDateTime.now().plusDays(1), ClientFingerprint.unknown());

        secret.revoke();

        assertThat(secret.isRevoked()).isTrue();
    }

    @Test
    void isExpiredComparesAgainstGivenInstant() {
        RefreshSecret secret = RefreshSecret.newSecret(UUID.randomUUID(), UUID.randomUUID(),
                "hash-value", LocalDateTime.of(2026, 10, 27, 12, 0), ClientFingerprint.unknown());

        assertThat(secret.isExpired(LocalDateTime.of(2026, 10, 27, 11, 59, 59))).isFalse();
        assertThat(secret.isExpired(LocalDateTime.of(2026, 10, 27, 12, 0))).isFalse();
        assertThat(secret.isExpired(LocalDateTime.of(2026, 10, 27, 12, 0, 1))).isTrue();
    }
}
