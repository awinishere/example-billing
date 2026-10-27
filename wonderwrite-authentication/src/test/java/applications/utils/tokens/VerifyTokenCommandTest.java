package applications.utils.tokens;

import applications.extensions.types.TokenType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerifyTokenCommandTest {

    @Test
    void acceptsCompleteCommand() {
        VerifyTokenCommand command = new VerifyTokenCommand("token-value", TokenType.ACCESS);

        assertThat(command.token()).isEqualTo("token-value");
        assertThat(command.expected()).isEqualTo(TokenType.ACCESS);
    }

    @Test
    void rejectsBlankToken() {
        assertThatThrownBy(() -> new VerifyTokenCommand(" ", TokenType.ACCESS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token must not be blank");
    }

    @Test
    void rejectsNullExpectedType() {
        assertThatThrownBy(() -> new VerifyTokenCommand("token-value", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected token type must not be null");
    }
}
