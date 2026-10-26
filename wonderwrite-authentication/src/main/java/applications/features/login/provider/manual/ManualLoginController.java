package applications.features.login.provider.manual;

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
@RequestMapping("/api/v1/login")
@AllArgsConstructor
@Tag(name = "Login", description = "Authenticate an account and establish a session.")
public class ManualLoginController {

    private final ManualLoginService manualLoginService;
    private final AuthCookieWriter cookieWriter;

    @Operation(
            summary = "Log in with email and password",
            description = """
                    Authenticates a manual account. Two successful outcomes share status 200: \
                    (1) **verified email** -> session cookies are set and `otpRequired=false`; \
                    (2) **unverified email** -> an OTP is emailed and `otpRequired=true` with `otpTtlSeconds`, \
                    **no cookies are set** until the code is confirmed at `POST /api/v1/login/verify-otp`. \
                    Invalid credentials return 401 with an empty body (never revealing whether the email exists). \
                    Repeated failures within a window return 429 guarded per email and per IP.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ManualLoginRequest.class),
                    examples = @ExampleObject(name = "valid", summary = "Email + password", value = """
                            {
                                "email":"newuser@example.com",
                                "password":"S3cret!passphrase"
                            }
                            """)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password correct. Body carries otpRequired=true with a TTL when the email is still unverified (no cookies set, finish at /api/v1/login/verify-otp); otherwise cookies are issued and otpRequired=false",
                    headers = @Header(name = HttpHeaders.SET_COOKIE, description = "HttpOnly access_token and refresh_token cookies, present only when the session is established immediately"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ManualLoginResponse.class),
                            examples = {
                                    @ExampleObject(name = "authenticated", summary = "verified email, cookies issued", value = """
                                            {
                                                "id":"7c9e6679-7425-40de-944b-e07fc1f90ae7",
                                                "email":"user@example.com",
                                                "otpRequired":false,
                                                "otpTtlSeconds":null
                                            }
                                            """),
                                    @ExampleObject(name = "otp-challenge", summary = "unverified email, OTP emailed", value = """
                                            {
                                                "id":"7c9e6679-7425-40de-944b-e07fc1f90ae7",
                                                "email":"newuser@example.com",
                                                "otpRequired":true,
                                                "otpTtlSeconds":300
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "Validation failed (blank/malformed email or password)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = applications.security.ErrorResponse.class),
                            examples = @ExampleObject(name = "validation", value = """
                                    {
                                        "reason":"VALIDATION_FAILED",
                                        "message":"email: must be a well-formed email address"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password; response body is empty",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts for this email or IP; retry after the cooldown",
                    headers = @Header(name = HttpHeaders.RETRY_AFTER, description = "Seconds until the rate-limit window clears"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = applications.security.ErrorResponse.class),
                            examples = @ExampleObject(name = "rate-limited", value = """
                                    {
                                        "reason":"RATE_LIMITED",
                                        "message":"Too many failed login attempts"
                                    }
                                    """)))
    })
    @PostMapping
    public ResponseEntity<ManualLoginResponse> login(
            @Valid
            @RequestBody
            ManualLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        ManualLoginResult result = manualLoginService.execute(
                new ManualLoginCommand(
                        request.email(),
                        request.password(),
                        ClientInfo.fingerprint(httpRequest)
                )
        );

        return switch (result) {
            case ManualLoginResult.Success success -> {
                HttpHeaders headers = new HttpHeaders();
                cookieWriter.apply(headers, success.tokens());
                yield ResponseEntity
                        .ok()
                        .headers(headers)
                        .body(ManualLoginResponse.from(success));
            }
            case ManualLoginResult.OtpChallenge challenge -> ResponseEntity
                    .ok()
                    .body(ManualLoginResponse.from(challenge));
            case ManualLoginResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        };
    }
}
