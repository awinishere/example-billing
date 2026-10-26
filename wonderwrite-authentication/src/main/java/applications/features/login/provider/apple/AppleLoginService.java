package applications.features.login.provider.apple;

import applications.features.register.provider.apple.AppleRegisterCommand;
import applications.features.register.provider.apple.AppleRegisterResult;
import applications.features.register.provider.apple.AppleRegisterService;
import applications.tokens.RefreshTokenIssuer;
import applications.tokens.command.StartTokenFamilyCommand;
import applications.tokens.result.StartTokenFamilyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppleLoginService {

    private final AppleRegisterService registerService;
    private final RefreshTokenIssuer refreshTokenIssuer;

    public AppleLoginResult login(AppleLoginCommand command) {
        AppleRegisterResult registered = registerService.register(
                new AppleRegisterCommand(command.providerUserId(), command.email())
        );

        StartTokenFamilyResult family = refreshTokenIssuer.startFamily(
                new StartTokenFamilyCommand(
                        registered.userId(),
                        registered.email(),
                        registered.role(),
                        command.fingerprint()
                )
        );

        return new AppleLoginResult(
                registered.userId(),
                family.tokens(),
                registered.provisioned()
        );
    }
}
