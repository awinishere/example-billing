package applications.utils.tokens;

import applications.extensions.types.TokenType;

public record VerifyTokenCommand(
        String token,
        TokenType expected
) {
    public VerifyTokenCommand {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be blank");
        }
        if (expected == null) {
            throw new IllegalArgumentException("Expected token type must not be null");
        }
    }
}
