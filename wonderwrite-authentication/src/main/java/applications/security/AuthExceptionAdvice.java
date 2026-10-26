package applications.security;

import applications.extensions.exception.RateLimitedException;
import applications.extensions.exception.TokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class AuthExceptionAdvice {

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ErrorResponse> onRateLimited(RateLimitedException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(
                HttpHeaders.RETRY_AFTER,
                String.valueOf(Math.max(1, ex.getRetryAfter().toSeconds()))
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(new ErrorResponse("RATE_LIMITED", ex.getMessage()));
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ErrorResponse> onToken(TokenException ex) {
        log.debug("Token rejected: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_TOKEN", "Token is invalid or expired"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> onValidation(MethodArgumentNotValidException ex) {
        String detail = ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("VALIDATION_FAILED", detail.isBlank() ? "Invalid input" : detail));
    }
}
