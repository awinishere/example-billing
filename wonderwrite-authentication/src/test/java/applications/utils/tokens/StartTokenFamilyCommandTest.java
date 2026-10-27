package applications.utils.tokens;

import applications.domain.model.ClientFingerprint;
import applications.extensions.types.RoleAccount;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartTokenFamilyCommandTest {

    @Test
    void acceptsCompleteCommand() {
        UUID userId = UUID.randomUUID();
        ClientFingerprint fingerprint = new ClientFingerprint("agent", "10.0.0.1");

        StartTokenFamilyCommand command = new StartTokenFamilyCommand(
                userId,
                "user@example.com",
                RoleAccount.USER,
                fingerprint
        );

        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.email()).isEqualTo("user@example.com");
        assertThat(command.role()).isEqualTo(RoleAccount.USER);
        assertThat(command.fingerprint()).isEqualTo(fingerprint);
    }

    @Test
    void allowsNullFingerprint() {
        StartTokenFamilyCommand command = new StartTokenFamilyCommand(
                UUID.randomUUID(),
                "user@example.com",
                RoleAccount.USER,
                null
        );

        assertThat(command.fingerprint()).isNull();
    }

    @Test
    void rejectsNullUserId() {
        assertThatThrownBy(() -> new StartTokenFamilyCommand(
                null,
                "user@example.com",
                RoleAccount.USER,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User id must not be null");
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> new StartTokenFamilyCommand(UUID.randomUUID(), " ", RoleAccount.USER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email must not be blank");
    }

    @Test
    void rejectsNullRole() {
        assertThatThrownBy(() -> new StartTokenFamilyCommand(UUID.randomUUID(), "user@example.com", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role must not be null");
    }
}
