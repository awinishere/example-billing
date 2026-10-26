package applications.configurations;

import applications.properties.OAuthProperties;
import applications.properties.OAuthProperties.Apple;
import applications.properties.OAuthProperties.Google;
import applications.utils.tokens.AppleClientSecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class OAuthClientConfiguration {

    private final OAuthProperties props;
    private final AppleClientSecretGenerator appleSecrets;

    @Bean
    @ConditionalOnExpression(
            "${app.security.oauth.google.enabled:false} or ${app.security.oauth.apple.enabled:false}"
    )
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>(2);

        Google google = props.getGoogle();
        if (google.isEnabled()) {
            registrations.add(google(google));
        }

        Apple apple = props.getApple();
        if (apple.isEnabled()) {
            registrations.add(apple(apple));
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private ClientRegistration google(Google google) {
        return ClientRegistration.withRegistrationId("google")
                .clientId(google.getClientId())
                .clientSecret(google.getClientSecret())
                .redirectUri(google.getRedirectUri())
                .scope(OidcScopes.OPENID, OidcScopes.EMAIL, OidcScopes.PROFILE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .issuerUri("https://accounts.google.com")
                .build();
    }

    private ClientRegistration apple(Apple apple) {
        return ClientRegistration.withRegistrationId("apple")
                .clientId(apple.getClientId())
                .clientSecret(appleSecrets.generate(apple))
                .redirectUri(apple.getRedirectUri())
                .authorizationUri("https://appleid.apple.com/auth/authorize?response_mode=form_post")
                .tokenUri("https://appleid.apple.com/auth/token")
                .jwkSetUri("https://appleid.apple.com/auth/keys")
                .scope(OidcScopes.OPENID, OidcScopes.EMAIL, "name")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .userInfoUri(null)
                .userNameAttributeName("sub")
                .clientName("Apple")
                .build();
    }
}
