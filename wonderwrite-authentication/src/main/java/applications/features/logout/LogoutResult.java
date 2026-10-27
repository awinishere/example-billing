package applications.features.logout;

public sealed interface LogoutResult {

    record Success(
            int sessions
    ) implements LogoutResult {}

    record Failed(
            String reason
    ) implements LogoutResult {}
}
