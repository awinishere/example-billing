package applications.features.password.reset;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reset-password")
@AllArgsConstructor
@Tag(name = "Password recovery", description = "Request a password reset and complete it with an OTP code.")
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    @Operation(
            summary = "Complete password reset",
            description = """
                    Sets a new password using a valid PASSWORD_RESET OTP. On success the password is re-hashed with Argon2id, \
                    the code is consumed, and **all active refresh sessions for the account are revoked** (forced re-login everywhere). \
                    A wrong, expired, or consumed code returns 400 with an empty body.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ResetPasswordRequest.class),
                    examples = {
                            @ExampleObject(name = "valid", summary = "Accepted code", value = """
                                    {
                                        "email":"user@example.com",
                                        "code":"482913",
                                        "newPassword":"N3w!passphrase"
                                    }
                                    """),
                            @ExampleObject(name = "invalid-code", summary = "Rejected with 400", value = """
                                    {
                                        "email":"user@example.com",
                                        "code":"000000",
                                        "newPassword":"N3w!passphrase"
                                    }
                                    """)
                    }))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset and all sessions revoked",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResetPasswordResponse.class),
                            examples = @ExampleObject(name = "reset", value = """
                                    {
                                        "id":"7c9e6679-7425-40de-944b-e07fc1f90ae7"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid/expired/consumed code (empty body) or request validation failure (ErrorResponse body)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = applications.security.ErrorResponse.class),
                            examples = @ExampleObject(name = "validation", value = """
                                    {
                                        "reason":"VALIDATION_FAILED",
                                        "message":"newPassword: must not be blank"
                                    }
                                    """)))
    })
    @PostMapping
    public ResponseEntity<ResetPasswordResponse> reset(
            @Valid
            @RequestBody
            ResetPasswordRequest request
    ) {
        return switch (resetPasswordService.execute(request.toCommand())) {
            case ResetPasswordResult.Success success -> ResponseEntity
                    .ok()
                    .body(ResetPasswordResponse.from(success));
            case ResetPasswordResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        };
    }
}
