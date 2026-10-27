package applications.features.login.guard;

public record RecordLoginAttemptCommand(
        String email,
        String ip,
        boolean succeeded
) {
}
