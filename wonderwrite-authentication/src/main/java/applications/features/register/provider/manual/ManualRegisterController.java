package applications.features.register.provider.manual;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ManualRegisterController.REGISTER_PATH)
@AllArgsConstructor
public class ManualRegisterController {

    public static final String REGISTER_PATH = "/api/v1/register";

    private final ManualRegisterService manualRegisterService;

    @PostMapping
    public ResponseEntity<ManualRegisterResponse> register(
            @Valid
            @RequestBody
            ManualRegisterRequest request
    ) {
        ManualRegisterResult result = manualRegisterService.execute(request.toCommand());

        return switch (result) {
            case ManualRegisterResult.Success success -> ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ManualRegisterResponse.from(success));
            case ManualRegisterResult.Failed failed -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        };
    }
}
