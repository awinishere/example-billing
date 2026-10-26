package applications.features.login.provider.google;

import applications.features.register.provider.google.GoogleRegisterCommand;
import applications.features.register.provider.google.GoogleRegisterResult;
import applications.features.register.provider.google.GoogleRegisterService;
import applications.tokens.RefreshTokenIssuer;
import applications.tokens.command.StartTokenFamilyCommand;
import applications.tokens.result.StartTokenFamilyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleLoginService {

    private final GoogleRegisterService registerService;
    private final RefreshTokenIssuer refreshTokenIssuer;

    public GoogleLoginResult login(GoogleLoginCommand command) {
        GoogleRegisterResult registered = registerService.register(
                new GoogleRegisterCommand(command.providerUserId(), command.email())
        );

        StartTokenFamilyResult family = refreshTokenIssuer.startFamily(
                new StartTokenFamilyCommand(
                        registered.userId(),
                        registered.email(),
                        registered.role(),
                        command.fingerprint()
                )
        );

        return new GoogleLoginResult(
                registered.userId(),
                family.tokens(),
                registered.provisioned()
        );
    }
}
