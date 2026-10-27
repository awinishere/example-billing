package applications.domain.repositories;

import applications.domain.entities.TestEntities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoginAttemptRepositoriesTest {

    @Autowired
    private LoginAttemptRepositories repositories;

    @Test
    void countsOnlyFailedAttemptsInsideTheWindow() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(15);
        repositories.saveAndFlush(TestEntities.loginAttempt("user@example.com", "10.0.0.1", false, since.plusMinutes(1)));
        repositories.saveAndFlush(TestEntities.loginAttempt("user@example.com", "10.0.0.2", false, since.plusMinutes(2)));
        repositories.saveAndFlush(TestEntities.loginAttempt("user@example.com", "10.0.0.3", true, since.plusMinutes(3)));
        repositories.saveAndFlush(TestEntities.loginAttempt("user@example.com", "10.0.0.4", false, since.minusMinutes(1)));
        repositories.saveAndFlush(TestEntities.loginAttempt("other@example.com", "10.0.0.5", false, since.plusMinutes(4)));

        long failed = repositories.countFailedByEmailSince("user@example.com", since);

        assertThat(failed).isEqualTo(2);
    }

    @Test
    void countsAllAttemptsWithinIpWindow() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(15);
        repositories.saveAndFlush(TestEntities.loginAttempt("user@example.com", "10.0.0.1", true, since.plusMinutes(1)));
        repositories.saveAndFlush(TestEntities.loginAttempt("user@example.com", "10.0.0.1", false, since.plusMinutes(2)));
        repositories.saveAndFlush(TestEntities.loginAttempt("user@example.com", "10.0.0.2", false, since.minusMinutes(1)));

        long attempts = repositories.countAttemptsByIpSince("10.0.0.1", since);

        assertThat(attempts).isEqualTo(2);
    }

    @Test
    void emptyHistoryCountsZero() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(15);

        assertThat(repositories.countFailedByEmailSince("nobody@example.com", since)).isZero();
        assertThat(repositories.countAttemptsByIpSince("192.0.2.9", since)).isZero();
    }
}
