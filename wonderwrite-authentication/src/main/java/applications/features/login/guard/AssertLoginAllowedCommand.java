package applications.features.login.guard;

public record AssertLoginAllowedCommand(
        String email,
        String ip
) {
}
