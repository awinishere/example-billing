package applications.features.register.provider.google;

public record GoogleRegisterCommand(
        String providerUserId,
        String email
) {
    public GoogleRegisterCommand {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("Google subject must not be blank");
        }
        providerUserId = providerUserId.trim();
        if (email != null) {
            email = email.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
