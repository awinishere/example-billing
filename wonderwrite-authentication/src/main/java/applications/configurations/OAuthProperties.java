package applications.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.oauth")
public class OAuthProperties {

    private Google google = new Google();
    private Apple apple = new Apple();
    private String successUrl = "/";
    private String failureUrl = "/login?error=oauth";

    @Getter
    @Setter
    public static class Google {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;
        private String redirectUri = "{baseUrl}/login/oauth2/code/{registrationId}";
    }

    @Getter
    @Setter
    public static class Apple {
        private boolean enabled = false;
        private String clientId;
        private String teamId;
        private String keyId;
        private String privateKeyPem;
        private String redirectUri = "{baseUrl}/login/oauth2/code/{registrationId}";
    }
}
