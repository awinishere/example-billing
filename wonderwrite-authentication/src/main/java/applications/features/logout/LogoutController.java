package applications.features.logout;

import applications.security.AuthCookieWriter;
import applications.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logout")
@AllArgsConstructor
@Tag(name = "Session", description = "Manage an authenticated session.")
public class LogoutController {

    private final LogoutService logoutService;
    private final AuthCookieWriter cookieWriter;

    @Operation(
            summary = "Log out and revoke all sessions",
            description = """
                    Ends the caller's session **on every device**: the `userId` is taken from the validated access token \
                    (never from the request payload), all of the account's un-revoked refresh-token families are marked \
                    revoked, and both auth cookies are cleared via `Set-Cookie` with `Max-Age=0`. \
                    Idempotent: logging out again returns `sessionsRevoked=0`. \
                    Requires a valid `access_token` cookie or `Authorization: Bearer <jwt>` header; otherwise 401.
                    """)
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "accessTokenCookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session terminated and all refresh sessions revoked",
                    headers = @Header(name = HttpHeaders.SET_COOKIE, description = "access_token and refresh_token cleared with Max-Age=0"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LogoutResponse.class),
                            examples = {
                                    @ExampleObject(name = "revoked", summary = "First logout", value = """
                                            {
                                                "sessionsRevoked": 2
                                            }
                                            """),
                                    @ExampleObject(name = "idempotent", summary = "Already logged out", value = """
                                            {
                                                "sessionsRevoked": 0
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "Missing, expired or invalid access token (ErrorResponse body from the security entry point)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = applications.security.ErrorResponse.class),
                            examples = @ExampleObject(name = "unauthenticated", value = """
                                    {
                                        "reason": "UNAUTHENTICATED",
                                        "message": "Authentication required"
                                    }
                                    """)))
    })
    @PostMapping
    public ResponseEntity<LogoutResponse> logout(@AuthenticationPrincipal AuthPrincipal principal) {
        HttpHeaders headers = new HttpHeaders();

        if (principal == null) {
            cookieWriter.clear(headers);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .headers(headers)
                    .build();
        }

        return switch (logoutService.execute(new LogoutCommand(principal.userId()))) {
            case LogoutResult.Success success -> {
                cookieWriter.clear(headers);
                yield ResponseEntity
                        .ok()
                        .headers(headers)
                        .body(LogoutResponse.from(success));
            }
            case LogoutResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .headers(headers)
                    .build();
        };
    }
}
