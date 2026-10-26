package applications.features.reset.password;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/forgot-password")
@AllArgsConstructor
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping
    public ResponseEntity<Void> forgot(
            @Valid
            @RequestBody
            ForgotPasswordRequest request
    ) {
        return switch (forgotPasswordService.execute(request.toCommand())) {
            case ForgotPasswordResult.Success success -> ResponseEntity.ok().build();
            case ForgotPasswordResult.Failed failed -> {
                log.warn("Forgot-password failed reason={}", failed.reason());
                yield ResponseEntity.ok().build();
            }
        };
    }
}
