package applications.domain.repositories;

import applications.domain.entities.OneTimePassCode;
import applications.extensions.types.OneTimePassPurpose;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OneTimePassCodeRepositoriesTest {

    @Autowired
    private OneTimePassCodeRepositories repositories;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsLatestUnconsumedCodeForUserAndPurpose() {
        UUID userId = UUID.randomUUID();
        OneTimePassCode older = repositories.saveAndFlush(newCode(userId, OneTimePassPurpose.LOGIN_VERIFICATION, "older-hash"));
        OneTimePassCode newer = repositories.saveAndFlush(newCode(userId, OneTimePassPurpose.LOGIN_VERIFICATION, "newer-hash"));
        stampCreatedAt(older, LocalDateTime.now().minusMinutes(2));
        stampCreatedAt(newer, LocalDateTime.now().minusMinutes(1));

        Optional<OneTimePassCode> found = repositories
                .findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(userId, OneTimePassPurpose.LOGIN_VERIFICATION);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(newer.getId());
        assertThat(found.get().getCodeHash()).isEqualTo("newer-hash");
    }

    @Test
    void consumedCodesAreExcluded() {
        UUID userId = UUID.randomUUID();
        OneTimePassCode code = repositories.saveAndFlush(newCode(userId, OneTimePassPurpose.PASSWORD_RESET, "hash-value"));
        code.markConsumed();
        repositories.saveAndFlush(code);

        assertThat(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.PASSWORD_RESET)).isEmpty();
    }

    @Test
    void otherPurposeIsNotMatched() {
        UUID userId = UUID.randomUUID();
        repositories.saveAndFlush(newCode(userId, OneTimePassPurpose.PASSWORD_RESET, "hash-value"));

        assertThat(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                userId, OneTimePassPurpose.LOGIN_VERIFICATION)).isEmpty();
    }

    @Test
    void otherUserIsNotMatched() {
        repositories.saveAndFlush(newCode(UUID.randomUUID(), OneTimePassPurpose.LOGIN_VERIFICATION, "hash-value"));

        assertThat(repositories.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                UUID.randomUUID(), OneTimePassPurpose.LOGIN_VERIFICATION)).isEmpty();
    }

    private void stampCreatedAt(OneTimePassCode code, LocalDateTime createdAt) {
        entityManager.createNativeQuery("update private_identity.otp_code set created_at = :createdAt where id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", code.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private OneTimePassCode newCode(UUID userId, OneTimePassPurpose purpose, String codeHash) {
        return OneTimePassCode.newCode(userId, purpose, codeHash, LocalDateTime.now().plusMinutes(10));
    }
}
