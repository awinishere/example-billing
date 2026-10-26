package applications.features.password.forgot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/forgot-password")
@AllArgsConstructor
@Tag(name = "Password recovery", description = "Request a password reset and complete it with an OTP code.")
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @Operation(
            summary = "Request a password reset code",
            description = """
                    Emails a one-time password-reset code when the address belongs to an account. \
                    **Enumeration-safe:** always returns 200 with an empty body, whether or not the email exists, \
                    and even when a send is throttled. The actual dispatch outcome is only logged server-side, never revealed to the caller.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ForgotPasswordRequest.class),
                    examples = {
                            @ExampleObject(name = "known-email", summary = "Address exists", value = """
                                    {"email":"user@example.com"}
                                    """),
                            @ExampleObject(name = "unknown-email", summary = "Address does not exist (still 200)", value = """
                                    {"email":"ghost@nowhere.com"}
                                    """)
                    }))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accepted unconditionally; body is empty to avoid leaking account existence",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Validation failed (blank or malformed email)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = applications.security.ErrorResponse.class),
                            examples = @ExampleObject(name = "validation", value = """
                                    {"reason":"VALIDATION_FAILED","message":"email: must be a well-formed email address"}
                                    """)))
    })
    @PostMapping
    public ResponseEntity<Void> forgot(
            @Valid
            @RequestBody
            ForgotPasswordRequest request
    ) {
        return switch (forgotPasswordService.execute(request.toCommand())) {
            case ForgotPasswordResult.Success success -> ResponseEntity.ok().build();
            case ForgotPasswordResult.Failed failed -> {
                log.warn("Forgot-password failed reason={}", failed.reason());
                yield ResponseEntity.ok().build();
            }
        };
    }
}
