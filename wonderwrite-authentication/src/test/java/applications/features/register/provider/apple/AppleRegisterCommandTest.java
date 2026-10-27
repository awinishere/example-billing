package applications.features.register.provider.apple;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleRegisterCommandTest {

    @Test
    void trimsSubjectAndNormalizesEmail() {
        AppleRegisterCommand command = new AppleRegisterCommand(
                " 001429.abc.def ",
                " User@Icloud.COM "
        );

        assertThat(command.providerUserId()).isEqualTo("001429.abc.def");
        assertThat(command.email()).isEqualTo("user@icloud.com");
    }

    @Test
    void allowsNullEmail() {
        AppleRegisterCommand command = new AppleRegisterCommand(
                "001429.abc.def",
                null
        );

        assertThat(command.email()).isNull();
    }

    @Test
    void rejectsBlankSubject() {
        assertThatThrownBy(() -> new AppleRegisterCommand(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apple subject must not be blank");
    }

    @Test
    void rejectsNullSubject() {
        assertThatThrownBy(() -> new AppleRegisterCommand(
                null, "user@icloud.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apple subject must not be blank");
    }
}
