package applications.features.register.provider.manual;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualRegisterCommandTest {

    private static final String PASSWORD = "S3cret!passphrase";

    @Test
    void normalizesEmailToLowerCaseTrimmed() {
        ManualRegisterCommand command = new ManualRegisterCommand("  User@Example.COM ", PASSWORD, PASSWORD);

        assertThat(command.email()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> new ManualRegisterCommand(" ", PASSWORD, PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email must not be blank");
    }

    @Test
    void rejectsNullEmail() {
        assertThatThrownBy(() -> new ManualRegisterCommand(null, PASSWORD, PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email must not be blank");
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> new ManualRegisterCommand("user@example.com", " ", PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must not be blank");
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> new ManualRegisterCommand("user@example.com", "short", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must be at least 8 characters");
    }

    @Test
    void rejectsMismatchedConfirmation() {
        assertThatThrownBy(() -> new ManualRegisterCommand("user@example.com", PASSWORD, "different"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password confirmation does not match");
    }
}
