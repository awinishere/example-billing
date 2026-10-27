package applications.features.login.provider.google;

import applications.domain.model.ClientFingerprint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleLoginCommandTest {

    @Test
    void trimsProviderUserId() {
        ClientFingerprint fingerprint = new ClientFingerprint("agent", "10.0.0.1");

        GoogleLoginCommand command = new GoogleLoginCommand(
                "  110483738369414381905 ",
                "user@gmail.com",
                fingerprint
        );

        assertThat(command.providerUserId()).isEqualTo("110483738369414381905");
        assertThat(command.email()).isEqualTo("user@gmail.com");
        assertThat(command.fingerprint()).isEqualTo(fingerprint);
    }

    @Test
    void rejectsBlankSubject() {
        assertThatThrownBy(() -> new GoogleLoginCommand(
                " ",
                "user@gmail.com",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google subject must not be blank");
    }

    @Test
    void rejectsNullSubject() {
        assertThatThrownBy(() -> new GoogleLoginCommand(
                null,
                "user@gmail.com",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google subject must not be blank");
    }
}
