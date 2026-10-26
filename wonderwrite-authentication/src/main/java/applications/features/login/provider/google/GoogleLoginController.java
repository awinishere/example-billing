package applications.features.login.provider.google;

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
@RequestMapping("/api/v1/login/google")
@AllArgsConstructor
@Tag(name = "Login", description = "Authenticate an account and establish a session.")
public class GoogleLoginController {

    private final GoogleLoginService googleLoginService;
    private final AuthCookieWriter cookieWriter;

    @Operation(
            summary = "Log in via Google",
            description = """
                    Authenticates an existing Google account by its provider subject id. \
                    Google-verified emails skip the OTP step-up, so a successful call immediately issues \
                    `access_token` and `refresh_token` HttpOnly cookies. Unknown identity returns 401 with an empty body.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = GoogleLoginRequest.class),
                    examples = @ExampleObject(name = "valid", summary = "Google identity", value = """
                            {
                                "providerUserId":"110483738369414381905",
                                "email":"user@gmail.com"
                            }
                            """)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated: session cookies issued",
                    headers = @Header(name = HttpHeaders.SET_COOKIE, description = "HttpOnly access_token and refresh_token cookies"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GoogleLoginResponse.class),
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
            @ApiResponse(responseCode = "401", description = "Unknown Google identity; response body is empty",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping
    public ResponseEntity<GoogleLoginResponse> login(
            @Valid
            @RequestBody
            GoogleLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        GoogleLoginResult result = googleLoginService.login(
                new GoogleLoginCommand(
                        request.providerUserId(),
                        request.email(),
                        ClientInfo.fingerprint(httpRequest)
                )
        );

        return switch (result) {
            case GoogleLoginResult.Success success -> {
                HttpHeaders headers = new HttpHeaders();
                cookieWriter.apply(headers, success.tokens());
                yield ResponseEntity
                        .ok()
                        .headers(headers)
                        .body(GoogleLoginResponse.from(success));
            }
            case GoogleLoginResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        };
    }
}
