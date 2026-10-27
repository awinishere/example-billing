package applications.features.register.provider.google;

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
@RequestMapping("/api/v1/register/google")
@AllArgsConstructor
@Tag(name = "Registration", description = "Create a new Wonderwrite account.")
public class GoogleRegisterController {

    private final GoogleRegisterService googleRegisterService;

    @Operation(
            summary = "Register via Google",
            description = """
                    Creates or links an account from a Google identity. `providerUserId` is the stable Google subject id; \
                    `provisioned=true` means a fresh account was created, `false` means an existing one was linked. \
                    Returns 409 when the Google identity or email is already bound to another credential set.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = GoogleRegisterRequest.class),
                    examples = @ExampleObject(name = "valid", summary = "New Google identity", value = """
                            {
                                "providerUserId":"110483738369414381905",
                                "email":"user@gmail.com"
                            }
                            """)))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Google account provisioned or linked",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GoogleRegisterResponse.class),
                            examples = @ExampleObject(name = "created", value = """
                                    {
                                        "id":"7c9e6679-7425-40de-944b-e07fc1f90ae7",
                                        "email":"user@gmail.com","provisioned":true
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Validation failed (blank providerUserId, malformed email)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = applications.security.ErrorResponse.class),
                            examples = @ExampleObject(name = "validation", value = """
                                    {
                                        "reason":"VALIDATION_FAILED",
                                        "message":"providerUserId: must not be blank"
                                    }
                                    """))),
            @ApiResponse(responseCode = "409", description = "Google identity or email already registered; response body is empty",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping
    public ResponseEntity<GoogleRegisterResponse> register(
            @Valid
            @RequestBody
            GoogleRegisterRequest request
    ) {
        GoogleRegisterResult result = googleRegisterService.register(request.toCommand());

        return switch (result) {
            case GoogleRegisterResult.Success success -> ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(GoogleRegisterResponse.from(success));
            case GoogleRegisterResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        };
    }
}
