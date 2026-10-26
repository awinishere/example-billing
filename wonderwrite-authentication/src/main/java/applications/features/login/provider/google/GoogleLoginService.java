package applications.features.login.provider.google;

import applications.features.register.provider.google.GoogleRegisterCommand;
import applications.features.register.provider.google.GoogleRegisterResult;
import applications.features.register.provider.google.GoogleRegisterService;
import applications.utils.tokens.RefreshTokenIssuer;
import applications.utils.tokens.StartTokenFamilyCommand;
import applications.utils.tokens.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleLoginService {

    private final GoogleRegisterService registerService;
    private final RefreshTokenIssuer refreshTokenIssuer;

    public GoogleLoginResult login(GoogleLoginCommand command) {
        return switch (registerService.register(
                new GoogleRegisterCommand(command.providerUserId(), command.email()))) {

            case GoogleRegisterResult.Failed failed ->
                    new GoogleLoginResult.Failed(failed.reason());

            case GoogleRegisterResult.Success success -> {
                TokenPair tokens = refreshTokenIssuer.startFamily(
                        new StartTokenFamilyCommand(
                                success.id(),
                                success.email(),
                                success.role(),
                                command.fingerprint()
                        )
                ).tokens();
                yield new GoogleLoginResult.Success(success.id(), tokens, success.provisioned());
            }
        };
    }
}
