package applications.crypto.features.hashing;

public record HashingCommand(
        String plainText
) {
    public HashingCommand {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("Plain text must not be empty");
        }
    }
}
