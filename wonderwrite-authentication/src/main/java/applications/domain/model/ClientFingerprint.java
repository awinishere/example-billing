package applications.domain.model;

public record ClientFingerprint(
        String userAgent,
        String ipAddress
) {
    public static ClientFingerprint unknown() {
        return new ClientFingerprint(
                null,
                null
        );
    }
}
