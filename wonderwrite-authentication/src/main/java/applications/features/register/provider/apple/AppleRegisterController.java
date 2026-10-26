package applications.features.register.provider.apple;

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
@RequestMapping( "/api/v1/register/apple")
@AllArgsConstructor
@Tag(name = "Registration", description = "Create a new Wonderwrite account.")
public class AppleRegisterController {

    private final AppleRegisterService appleRegisterService;

    @Operation(
            summary = "Register via Apple",
            description = """
                    Creates or links an account from a Sign in with Apple identity. `providerUserId` is the stable Apple \
                    subject id; `provisioned=true` means a fresh account was created, `false` means an existing one was linked. \
                    Returns 409 when the Apple identity or email is already bound to another credential set.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppleRegisterRequest.class),
                    examples = @ExampleObject(name = "valid", summary = "New Apple identity", value = """
                            {"providerUserId":"001429.3a8b7c6d5e4f210a9b8c","email":"user@icloud.com"}
                            """)))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Apple account provisioned or linked",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AppleRegisterResponse.class),
                            examples = @ExampleObject(name = "created", value = """
                                    {"id":"7c9e6679-7425-40de-944b-e07fc1f90ae7","email":"user@icloud.com","provisioned":true}
                                    """))),
            @ApiResponse(responseCode = "400", description = "Validation failed (blank providerUserId, malformed email)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = applications.security.ErrorResponse.class),
                            examples = @ExampleObject(name = "validation", value = """
                                    {"reason":"VALIDATION_FAILED","message":"providerUserId: must not be blank"}
                                    """))),
            @ApiResponse(responseCode = "409", description = "Apple identity or email already registered; response body is empty",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping
    public ResponseEntity<AppleRegisterResponse> register(
            @Valid
            @RequestBody
            AppleRegisterRequest request
    ) {
        AppleRegisterResult result = appleRegisterService.register(request.toCommand());

        return switch (result) {
            case AppleRegisterResult.Success success -> ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(AppleRegisterResponse.from(success));
            case AppleRegisterResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        };
    }
}
