package applications.utils.tokens;

import applications.extensions.types.RoleAccount;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueTokenCommandTest {

    @Test
    void acceptsCompleteCommand() {
        UUID userId = UUID.randomUUID();

        IssueTokenCommand command = new IssueTokenCommand(
                userId,
                "user@example.com",
                RoleAccount.USER
        );

        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.email()).isEqualTo("user@example.com");
        assertThat(command.role()).isEqualTo(RoleAccount.USER);
    }

    @Test
    void rejectsNullUserId() {
        assertThatThrownBy(() -> new IssueTokenCommand(
                null,
                "user@example.com",
                RoleAccount.USER
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User id must not be null");
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> new IssueTokenCommand(
                UUID.randomUUID(),
                "  ",
                RoleAccount.USER
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email must not be blank");
    }

    @Test
    void rejectsNullRole() {
        assertThatThrownBy(() -> new IssueTokenCommand(
                UUID.randomUUID(),
                "user@example.com",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role must not be null");
    }
}
