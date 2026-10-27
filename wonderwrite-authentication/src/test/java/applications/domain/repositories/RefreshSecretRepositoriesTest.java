package applications.domain.repositories;

import applications.domain.entities.RefreshSecret;
import applications.domain.model.ClientFingerprint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshSecretRepositoriesTest {

    @Autowired
    private RefreshSecretRepositories repositories;

    @Test
    void findByUserIdAndRevokedFalseSkipsRevokedSecrets() {
        UUID userId = UUID.randomUUID();
        RefreshSecret active = saveSecret(userId, "hash-active");
        RefreshSecret revoked = saveSecret(userId, "hash-revoked");
        revoked.revoke();
        repositories.saveAndFlush(revoked);
        saveSecret(UUID.randomUUID(), "hash-other-user");

        List<RefreshSecret> found = repositories.findByUserIdAndRevokedFalse(userId);

        assertThat(found).hasSize(1);
        assertThat(found).extracting("id").containsExactly(active.getId());
    }

    @Test
    void noActiveSecretsReturnsEmptyList() {
        assertThat(repositories.findByUserIdAndRevokedFalse(UUID.randomUUID())).isEmpty();
    }

    @Test
    void persistedSecretKeepsClientInfoAndHash() {
        UUID userId = UUID.randomUUID();
        RefreshSecret saved = saveSecret(userId, "hash-value");

        RefreshSecret found = repositories.findById(saved.getId()).orElseThrow();
        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.getSecretHash()).isEqualTo("hash-value");
        assertThat(found.getUserAgent()).isEqualTo("agent/1.0");
        assertThat(found.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(found.isRevoked()).isFalse();
    }

    private RefreshSecret saveSecret(UUID userId, String secretHash) {
        return repositories.saveAndFlush(RefreshSecret.newSecret(userId, UUID.randomUUID(), secretHash,
                LocalDateTime.now().plusDays(7), new ClientFingerprint("agent/1.0", "127.0.0.1")));
    }
}
