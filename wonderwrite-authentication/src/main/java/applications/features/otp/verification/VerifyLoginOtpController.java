package applications.features.otp.verification;

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
@RequestMapping("/api/v1/login/verify-otp")
@AllArgsConstructor
public class VerifyLoginOtpController {

    private final VerifyLoginOtpService verifyLoginOtpService;
    private final AuthCookieWriter cookieWriter;

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
