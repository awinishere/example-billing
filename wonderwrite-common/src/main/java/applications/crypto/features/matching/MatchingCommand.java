package applications.crypto.features.matching;

public record MatchingCommand(
        String plainText,
        String textHashing
) {
    public MatchingCommand {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("Plain text must not be empty");
        }
        if (textHashing == null || textHashing.isBlank()) {
            throw new IllegalArgumentException("Hash must not be blank");
        }
    }
}
