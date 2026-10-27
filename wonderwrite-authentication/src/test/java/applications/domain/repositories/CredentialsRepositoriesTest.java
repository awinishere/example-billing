package applications.domain.repositories;

import applications.domain.entities.Credentials;
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
class CredentialsRepositoriesTest {

    @Autowired
    private CredentialsRepositories repositories;

    @Test
    void savesGeneratedUuidIdAndReadsBackByEmail() {
        Credentials saved = repositories.save(Credentials.newAccount("user@example.com", "hash-value"));
        repositories.flush();

        assertThat(saved.getId()).isNotNull();

        Optional<Credentials> found = repositories.findByEmail("user@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().isEmailVerification()).isFalse();
    }

    @Test
    void findByEmailReturnsEmptyForUnknownEmail() {
        assertThat(repositories.findByEmail("ghost@example.com")).isEmpty();
    }

    @Test
    void existsByEmailReflectsStoredRows() {
        repositories.save(Credentials.newAccount("taken@example.com", "hash-value"));

        assertThat(repositories.existsByEmail("taken@example.com")).isTrue();
        assertThat(repositories.existsByEmail("free@example.com")).isFalse();
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        repositories.save(Credentials.newAccount("dup@example.com", "hash-one"));
        repositories.flush();

        assertThatThrownBy(() -> {
            repositories.saveAndFlush(Credentials.newAccount("dup@example.com", "hash-two"));
        }).isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void findByIdUsesUuidPrimaryKey() {
        Credentials saved = repositories.save(Credentials.newAccount("byid@example.com", "hash-value"));
        UUID id = saved.getId();

        assertThat(repositories.findById(id)).contains(saved);
        assertThat(repositories.findById(UUID.randomUUID())).isEmpty();
    }
}
