package applications.features.register.provider.google;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleRegisterCommandTest {

    @Test
    void trimsSubjectAndNormalizesEmail() {
        GoogleRegisterCommand command = new GoogleRegisterCommand(
                "  110483738369414381905 ",
                " User@Gmail.com "
        );

        assertThat(command.providerUserId()).isEqualTo("110483738369414381905");
        assertThat(command.email()).isEqualTo("user@gmail.com");
    }

    @Test
    void allowsNullEmail() {
        GoogleRegisterCommand command = new GoogleRegisterCommand(
                "110483738369414381905",
                null
        );

        assertThat(command.email()).isNull();
    }

    @Test
    void rejectsBlankSubject() {
        assertThatThrownBy(() -> new GoogleRegisterCommand(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google subject must not be blank");
    }

    @Test
    void rejectsNullSubject() {
        assertThatThrownBy(() -> new GoogleRegisterCommand(null, "user@gmail.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google subject must not be blank");
    }
}
