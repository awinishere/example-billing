package applications.features.reset.password;

public sealed interface ForgotPasswordResult {

    record Success(
            boolean dispatched,
            int ttlSeconds
    ) implements ForgotPasswordResult {}

    record Failed(
            String reason
    ) implements ForgotPasswordResult {}
}
