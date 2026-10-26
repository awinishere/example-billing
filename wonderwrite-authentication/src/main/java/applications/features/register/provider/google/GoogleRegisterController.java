package applications.features.register.provider.google;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/register/google")
@AllArgsConstructor
public class GoogleRegisterController {

    private final GoogleRegisterService googleRegisterService;

    @PostMapping
    public ResponseEntity<GoogleRegisterResponse> register(
            @Valid
            @RequestBody
            GoogleRegisterRequest request
    ) {
        GoogleRegisterResult result = googleRegisterService.register(request.toCommand());

        return switch (result) {
            case GoogleRegisterResult.Success success -> ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(GoogleRegisterResponse.from(success));
            case GoogleRegisterResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        };
    }
}
