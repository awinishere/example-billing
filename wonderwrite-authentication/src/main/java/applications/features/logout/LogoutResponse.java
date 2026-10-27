package applications.features.logout;

public record LogoutResponse(
        int sessionsRevoked
) {
    public static LogoutResponse from(LogoutResult.Success success) {
        return new LogoutResponse(success.sessions());
    }
}
