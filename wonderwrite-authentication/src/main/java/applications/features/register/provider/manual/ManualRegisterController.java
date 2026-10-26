package applications.features.register.provider.manual;

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
@RequestMapping("/api/v1/register")
@AllArgsConstructor
@Tag(name = "Registration", description = "Create a new Wonderwrite account.")
public class ManualRegisterController {

    private final ManualRegisterService manualRegisterService;

    @Operation(
            summary = "Register with email and password",
            description = """
                    Creates a manual (email + password) account. The password is hashed with Argon2id before storage; \
                    the plain password is never persisted or returned. `password` and `confirmPassword` must match. \
                    The created account starts with `emailVerification=false`, so the first login triggers an OTP step-up.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ManualRegisterRequest.class),
                    examples = {
                            @ExampleObject(name = "valid", summary = "Accepted payload", value = """
                                    {
                                        "email":"newuser@example.com",
                                        "password":"S3cret!passphrase",
                                        "confirmPassword":"S3cret!passphrase"
                                    }
                                    """),
                            @ExampleObject(name = "mismatch", summary = "confirmPassword differs (rejected with 409/400)", value = """
                                    {
                                        "email":"newuser@example.com",
                                        "password":"S3cret!passphrase",
                                        "confirmPassword":"different"
                                    }
                                    """)
                    }))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ManualRegisterResponse.class),
                            examples = @ExampleObject(name = "created", value = """
                                    {
                                        "id":"7c9e6679-7425-40de-944b-e07fc1f90ae7",
                                        "email":"newuser@example.com",
                                        "emailVerification":false
                                    }
                                    """)
                    )),
            @ApiResponse(responseCode = "400", description = "Validation failed (blank/malformed email, password too short, size limits exceeded)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = applications.security.ErrorResponse.class),
                            examples = @ExampleObject(name = "validation", value = """
                                    {
                                        "reason":"VALIDATION_FAILED",
                                        "message":"email: must be a well-formed email address"
                                    }
                                    """))),
            @ApiResponse(responseCode = "409", description = "Email is already registered; response body is empty",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping
    public ResponseEntity<ManualRegisterResponse> register(
            @Valid
            @RequestBody
            ManualRegisterRequest request
    ) {
        ManualRegisterResult result = manualRegisterService.execute(request.toCommand());

        return switch (result) {
            case ManualRegisterResult.Success success -> ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ManualRegisterResponse.from(success));
            case ManualRegisterResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        };
    }
}
