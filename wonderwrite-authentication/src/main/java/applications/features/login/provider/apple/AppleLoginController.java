package applications.features.login.provider.apple;

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
@RequestMapping("/api/v1/login/apple")
@AllArgsConstructor
public class AppleLoginController {

    private final AppleLoginService appleLoginService;
    private final AuthCookieWriter cookieWriter;

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
