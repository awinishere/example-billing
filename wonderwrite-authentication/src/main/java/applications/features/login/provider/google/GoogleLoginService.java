package applications.features.login.provider.google;

import applications.domain.entities.Credentials;
import applications.domain.entities.OAuthCredentials;
import applications.domain.repositories.CredentialsRepositories;
import applications.domain.repositories.OAuthCredentialsRepositories;
import applications.utils.tokens.RefreshTokenIssuer;
import applications.utils.tokens.StartTokenFamilyCommand;
import applications.utils.tokens.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleLoginService {

    private static final String PROVIDER = "google";

    private final OAuthCredentialsRepositories oAuthCredentialsRepositories;
    private final CredentialsRepositories credentialsRepositories;
    private final RefreshTokenIssuer refreshTokenIssuer;

    public GoogleLoginResult login(GoogleLoginCommand command) {
        Optional<OAuthCredentials> link = oAuthCredentialsRepositories
                .findByProviderAndProviderUserId(PROVIDER, command.providerUserId());

        if (link.isEmpty()) {
            return new GoogleLoginResult.Failed("Unknown Google identity");
        }

        Optional<Credentials> account = credentialsRepositories.findById(link.get().getUserId());
        if (account.isEmpty()) {
            return new GoogleLoginResult.Failed("OAuth link points at missing account");
        }

        Credentials credentials = account.get();
        TokenPair token = refreshTokenIssuer.startFamily(
                new StartTokenFamilyCommand(
                        credentials.getId(),
                        credentials.getEmail(),
                        credentials.getRole(),
                        command.fingerprint()
                )
        ).tokens();
        return new GoogleLoginResult.Success(
                credentials.getId(),
                token,
                false
        );
    }
}
