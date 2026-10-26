package applications.features.otp.verification;

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
@RequestMapping("/api/v1/login/verify-otp")
@AllArgsConstructor
@Tag(name = "Login", description = "Authenticate an account and establish a session.")
public class VerifyLoginOtpController {

    private final VerifyLoginOtpService verifyLoginOtpService;
    private final AuthCookieWriter cookieWriter;

    @Operation(
            summary = "Confirm login OTP and establish session",
            description = """
                    Completes the step-up challenge returned by `POST /api/v1/login` when the email was unverified. \
                    On success the email is marked verified and `access_token` + `refresh_token` HttpOnly cookies are issued. \
                    A wrong, expired, or already-consumed code returns 401 with an empty body; the reason is logged, never exposed.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = VerifyLoginOtpRequest.class),
                    examples = {
                            @ExampleObject(name = "valid", summary = "Correct code", value = """
                                    {"userId":"7c9e6679-7425-40de-944b-e07fc1f90ae7","code":"482913"}
                                    """),
                            @ExampleObject(name = "wrong-code", summary = "Rejected with 401", value = """
                                    {"userId":"7c9e6679-7425-40de-944b-e07fc1f90ae7","code":"000000"}
                                    """)
                    }))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code correct: email marked verified and session cookies issued",
                    headers = @Header(name = HttpHeaders.SET_COOKIE, description = "HttpOnly access_token and refresh_token cookies"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VerifyLoginOtpResponse.class),
                            examples = @ExampleObject(name = "verified", value = """
                                    {"id":"7c9e6679-7425-40de-944b-e07fc1f90ae7","email":"newuser@example.com"}
                                    """))),
            @ApiResponse(responseCode = "400", description = "Validation failed (null userId, code outside 4-10 chars)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = applications.security.ErrorResponse.class),
                            examples = @ExampleObject(name = "validation", value = """
                                    {"reason":"VALIDATION_FAILED","message":"code: size must be between 4 and 10"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or consumed code; response body is empty",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping
    public ResponseEntity<VerifyLoginOtpResponse> verify(
            @Valid
            @RequestBody
            VerifyLoginOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        VerifyLoginOtpResult result = verifyLoginOtpService.execute(
                new VerifyLoginOtpCommand(
                        request.userId(),
                        request.code(),
                        ClientInfo.fingerprint(httpRequest)
                )
        );

        return switch (result) {
            case VerifyLoginOtpResult.Success success -> {
                HttpHeaders headers = new HttpHeaders();
                cookieWriter.apply(headers, success.tokens());
                yield ResponseEntity
                        .ok()
                        .headers(headers)
                        .body(VerifyLoginOtpResponse.from(success));
            }
            case VerifyLoginOtpResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        };
    }
}
