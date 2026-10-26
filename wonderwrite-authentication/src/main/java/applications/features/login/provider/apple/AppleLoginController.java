package applications.features.login.provider.apple;

import applications.security.AuthCookieWriter;
import applications.security.ClientInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/login/apple")
@AllArgsConstructor
@Tag(name = "Login", description = "Authenticate an account and establish a session.")
public class AppleLoginController {

    private final AppleLoginService appleLoginService;
    private final AuthCookieWriter cookieWriter;

    @Operation(
            summary = "Log in via Apple",
            description = """
                    Authenticates an existing Sign in with Apple account by its provider subject id. \
                    Apple-verified emails skip the OTP step-up, so a successful call immediately issues \
                    `access_token` and `refresh_token` HttpOnly cookies. Unknown identity returns 401 with an empty body.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppleLoginRequest.class),
                    examples = @ExampleObject(name = "valid", summary = "Apple identity", value = """
                            {
                                "providerUserId":"001429.3a8b7c6d5e4f210a9b8c",
                                "email":"user@icloud.com"
                            }
                            """)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated: session cookies issued",
                    headers = @Header(name = HttpHeaders.SET_COOKIE, description = "HttpOnly access_token and refresh_token cookies"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AppleLoginResponse.class),
                            examples = @ExampleObject(name = "authenticated", value = """
                                    {
                                        "id":"7c9e6679-7425-40de-944b-e07fc1f90ae7",
                                        "provisioned":true
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
            @ApiResponse(responseCode = "401", description = "Unknown Apple identity; response body is empty",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping
    public ResponseEntity<AppleLoginResponse> login(
            @Valid
            @RequestBody
            AppleLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AppleLoginResult result = appleLoginService.login(
                new AppleLoginCommand(
                        request.providerUserId(),
                        request.email(),
                        ClientInfo.fingerprint(httpRequest)
                )
        );

        return switch (result) {
            case AppleLoginResult.Success success -> {
                HttpHeaders headers = new HttpHeaders();
                cookieWriter.apply(headers, success.tokens());
                yield ResponseEntity
                        .ok()
                        .headers(headers)
                        .body(AppleLoginResponse.from(success));
            }
            case AppleLoginResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        };
    }
}
