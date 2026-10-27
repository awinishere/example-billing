package applications.security;

import applications.extensions.exception.RateLimitedException;
import applications.extensions.exception.TokenException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionAdviceTest {

    private final AuthExceptionAdvice advice = new AuthExceptionAdvice();

    @Test
    void rateLimitedBecomes429WithRetryAfterSeconds() {
        ResponseEntity<ErrorResponse> response = advice.onRateLimited(
                new RateLimitedException("Too many login attempts", Duration.ofMinutes(15)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("900");
        assertThat(response.getBody().reason()).isEqualTo("RATE_LIMITED");
        assertThat(response.getBody().message()).isEqualTo("Too many login attempts");
    }

    @Test
    void subSecondRetryAfterIsFlooredToOneSecond() {
        ResponseEntity<ErrorResponse> response = advice.onRateLimited(
                new RateLimitedException("Slow down", Duration.ofMillis(400)));

        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("1");
    }

    @Test
    void tokenFailureBecomes401WithInvalidTokenReason() {
        ResponseEntity<ErrorResponse> response = advice.onToken(new TokenException("Token has expired"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().reason()).isEqualTo("INVALID_TOKEN");
        assertThat(response.getBody().message()).isEqualTo("Token is invalid or expired");
    }

    @Test
    void bindingViolationsBecome400WithJoinedFieldDetails() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "email", "must be a well-formed email address"));
        binding.addError(new FieldError("request", "password", "size must be between 8 and 72"));

        ResponseEntity<ErrorResponse> response = advice.onValidation(validException(binding));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().reason()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().message())
                .isEqualTo("email: must be a well-formed email address; password: size must be between 8 and 72");
    }

    @Test
    void validationWithoutFieldErrorsFallsBackToGenericMessage() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new ObjectError("request", "global failure"));

        ResponseEntity<ErrorResponse> response = advice.onValidation(validException(binding));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Invalid input");
    }

    @Test
    void commandContractViolationBecomes400WithReasonMessage() {
        ResponseEntity<ErrorResponse> response = advice.onIllegalArgument(
                new IllegalArgumentException("Password confirmation does not match"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().reason()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().message()).isEqualTo("Password confirmation does not match");
    }

    private MethodArgumentNotValidException validException(BeanPropertyBindingResult binding) throws Exception {
        MethodParameter parameter = new MethodParameter(AuthExceptionAdviceTest.class
                .getDeclaredMethod("sampleHandler", String.class), 0);
        return new MethodArgumentNotValidException(parameter, binding);
    }

    @SuppressWarnings("unused")
    private void sampleHandler(String ignored) {
    }
}
