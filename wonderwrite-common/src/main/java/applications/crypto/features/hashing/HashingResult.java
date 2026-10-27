package applications.crypto.features.hashing;

public record HashingResult(
        String hash
) {
    public HashingResult {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Hash must not be blank");
        }
    }
}
