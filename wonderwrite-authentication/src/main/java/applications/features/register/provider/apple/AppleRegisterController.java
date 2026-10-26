package applications.features.register.provider.apple;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/api/v1/register/apple")
@AllArgsConstructor
public class AppleRegisterController {

    private final AppleRegisterService appleRegisterService;

    @PostMapping
    public ResponseEntity<AppleRegisterResponse> register(
            @Valid
            @RequestBody
            AppleRegisterRequest request
    ) {
        AppleRegisterResult result = appleRegisterService.register(request.toCommand());

        return switch (result) {
            case AppleRegisterResult.Success success -> ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(AppleRegisterResponse.from(success));
            case AppleRegisterResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        };
    }
}
