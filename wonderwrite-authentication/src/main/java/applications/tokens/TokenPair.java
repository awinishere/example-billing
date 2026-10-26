package applications.tokens;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessTtlSeconds,
        long refreshTtlSeconds
) {
}
