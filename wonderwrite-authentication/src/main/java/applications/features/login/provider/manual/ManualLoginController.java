package applications.features.login.provider.manual;

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
@RequestMapping("/api/v1/login")
@AllArgsConstructor
public class ManualLoginController {

    private final ManualLoginService manualLoginService;
    private final AuthCookieWriter cookieWriter;

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
