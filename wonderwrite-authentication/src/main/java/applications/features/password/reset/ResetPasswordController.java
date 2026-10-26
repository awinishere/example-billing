package applications.features.password.reset;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reset-password")
@AllArgsConstructor
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    @PostMapping
    public ResponseEntity<ResetPasswordResponse> reset(
            @Valid
            @RequestBody
            ResetPasswordRequest request
    ) {
        return switch (resetPasswordService.execute(request.toCommand())) {
            case ResetPasswordResult.Success success -> ResponseEntity
                    .ok()
                    .body(ResetPasswordResponse.from(success));
            case ResetPasswordResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        };
    }
}
