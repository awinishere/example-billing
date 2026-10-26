package applications.features.login.provider.google;

import applications.security.AuthCookieWriter;
import applications.security.ClientInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/login/google")
@AllArgsConstructor
public class GoogleLoginController {

    private final GoogleLoginService googleLoginService;
    private final AuthCookieWriter cookieWriter;

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
