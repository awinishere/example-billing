package applications.domain.entities;

import applications.extensions.types.OneTimePassPurpose;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OneTimePassCodeTest {

    @Test
    void newCodeStartsFreshAndUnconsumed() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        OneTimePassCode code = OneTimePassCode.newCode(userId, OneTimePassPurpose.LOGIN_VERIFICATION,
                "hash-value", expiresAt);

        assertThat(code.getId()).isNull();
        assertThat(code.getUserId()).isEqualTo(userId);
        assertThat(code.getPurpose()).isEqualTo(OneTimePassPurpose.LOGIN_VERIFICATION);
        assertThat(code.getCodeHash()).isEqualTo("hash-value");
        assertThat(code.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(code.isConsumed()).isFalse();
        assertThat(code.getAttempts()).isZero();
    }

    @Test
    void markConsumedFlagsCodeSpent() {
        OneTimePassCode code = freshCode();

        code.markConsumed();

        assertThat(code.isConsumed()).isTrue();
    }

    @Test
    void registerFailedAttemptIncrementsCounter() {
        OneTimePassCode code = freshCode();

        code.registerFailedAttempt();
        code.registerFailedAttempt();
        code.registerFailedAttempt();

        assertThat(code.getAttempts()).isEqualTo(3);
    }

    @Test
    void isExpiredComparesAgainstGivenInstant() {
        OneTimePassCode code = OneTimePassCode.newCode(UUID.randomUUID(), OneTimePassPurpose.LOGIN_VERIFICATION,
                "hash-value", LocalDateTime.of(2026, 10, 27, 12, 0));

        assertThat(code.isExpired(LocalDateTime.of(2026, 10, 27, 11, 59, 59))).isFalse();
        assertThat(code.isExpired(LocalDateTime.of(2026, 10, 27, 12, 0, 1))).isTrue();
    }

    private OneTimePassCode freshCode() {
        return OneTimePassCode.newCode(UUID.randomUUID(), OneTimePassPurpose.PASSWORD_RESET,
                "hash-value", LocalDateTime.now().plusMinutes(10));
    }
}
