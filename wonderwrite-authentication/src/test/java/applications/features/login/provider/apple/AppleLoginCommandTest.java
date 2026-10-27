package applications.features.login.provider.apple;

import applications.domain.model.ClientFingerprint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleLoginCommandTest {

    @Test
    void trimsProviderUserId() {
        ClientFingerprint fingerprint = new ClientFingerprint("agent", "10.0.0.1");

        AppleLoginCommand command = new AppleLoginCommand(
                " 001429.abc.def ",
                "user@icloud.com",
                fingerprint
        );

        assertThat(command.providerUserId()).isEqualTo("001429.abc.def");
        assertThat(command.email()).isEqualTo("user@icloud.com");
        assertThat(command.fingerprint()).isEqualTo(fingerprint);
    }

    @Test
    void rejectsBlankSubject() {
        assertThatThrownBy(() -> new AppleLoginCommand(
                " ",
                "user@icloud.com",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apple subject must not be blank");
    }

    @Test
    void rejectsNullSubject() {
        assertThatThrownBy(() -> new AppleLoginCommand(
                null,
                "user@icloud.com",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apple subject must not be blank");
    }
}
