package applications.features.register.provider.apple;

public record AppleRegisterCommand(
        String providerUserId,
        String email
) {
    public AppleRegisterCommand {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("Apple subject must not be blank");
        }
        providerUserId = providerUserId.trim();
        if (email != null) {
            email = email.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
