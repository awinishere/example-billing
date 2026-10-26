package applications.features.login.provider.apple;

import applications.features.register.provider.apple.AppleRegisterCommand;
import applications.features.register.provider.apple.AppleRegisterResult;
import applications.features.register.provider.apple.AppleRegisterService;
import applications.utils.tokens.RefreshTokenIssuer;
import applications.utils.tokens.StartTokenFamilyCommand;
import applications.utils.tokens.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppleLoginService {

    private final AppleRegisterService registerService;
    private final RefreshTokenIssuer refreshTokenIssuer;

    public AppleLoginResult login(AppleLoginCommand command) {
        return switch (registerService.register(
                new AppleRegisterCommand(command.providerUserId(), command.email()))) {

            case AppleRegisterResult.Failed failed ->
                    new AppleLoginResult.Failed(failed.reason());

            case AppleRegisterResult.Success success -> {
                TokenPair tokens = refreshTokenIssuer.startFamily(
                        new StartTokenFamilyCommand(
                                success.id(),
                                success.email(),
                                success.role(),
                                command.fingerprint()
                        )
                ).tokens();
                yield new AppleLoginResult.Success(success.id(), tokens, success.provisioned());
            }
        };
    }
}
