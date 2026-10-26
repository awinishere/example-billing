package applications.tokens.result;

import applications.tokens.TokenClaims;

public record VerifyTokenResult(
        TokenClaims claims
) {
}
