package applications.security;

public record ErrorResponse(
        String reason,
        String message
) {
}
