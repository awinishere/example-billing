package applications.extensions.exception;

import lombok.Getter;

import java.time.Duration;

@Getter
public class RateLimitedException extends RuntimeException {

    private final Duration retryAfter;

    public RateLimitedException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }
}
