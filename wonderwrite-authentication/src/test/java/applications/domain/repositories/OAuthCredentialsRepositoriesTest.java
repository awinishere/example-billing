package applications.domain.repositories;

import applications.domain.entities.OAuthCredentials;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OAuthCredentialsRepositoriesTest {

    @Autowired
    private OAuthCredentialsRepositories repositories;

    @Test
    void findsLinkByProviderAndProviderUserId() {
        UUID userId = UUID.randomUUID();
        repositories.save(OAuthCredentials.newLink(userId, "google", "110483738369414381905", "user@gmail.com"));

        Optional<OAuthCredentials> found =
                repositories.findByProviderAndProviderUserId("google", "110483738369414381905");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getEmail()).isEqualTo("user@gmail.com");
    }

    @Test
    void sameProviderUserIdUnderDifferentProviderIsNotMatched() {
        repositories.save(OAuthCredentials.newLink(UUID.randomUUID(), "apple", "shared-subject", "user@icloud.com"));

        assertThat(repositories.findByProviderAndProviderUserId("google", "shared-subject")).isEmpty();
    }

    @Test
    void unknownSubjectReturnsEmpty() {
        assertThat(repositories.findByProviderAndProviderUserId("google", "does-not-exist")).isEmpty();
    }

    @Test
    void duplicateProviderPairViolatesUniqueConstraint() {
        repositories.save(OAuthCredentials.newLink(UUID.randomUUID(), "google", "dup-subject", "one@gmail.com"));
        repositories.flush();

        assertThatThrownBy(() -> repositories.saveAndFlush(
                OAuthCredentials.newLink(UUID.randomUUID(), "google", "dup-subject", "two@gmail.com")))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }
}
