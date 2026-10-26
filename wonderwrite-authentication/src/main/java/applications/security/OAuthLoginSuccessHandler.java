package applications.security;

import applications.configurations.OAuthProperties;
import applications.domain.model.ClientFingerprint;
import applications.features.login.provider.apple.AppleLoginCommand;
import applications.features.login.provider.apple.AppleLoginResult;
import applications.features.login.provider.apple.AppleLoginService;
import applications.features.login.provider.google.GoogleLoginCommand;
import applications.features.login.provider.google.GoogleLoginResult;
import applications.features.login.provider.google.GoogleLoginService;
import applications.tokens.TokenPair;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String GOOGLE = "google";
    private static final String APPLE = "apple";

    private final GoogleLoginService googleLoginService;
    private final AppleLoginService appleLoginService;
    private final AuthCookieWriter cookieWriter;
    private final OAuthProperties props;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            response.sendRedirect(props.getFailureUrl());
            return;
        }

        String provider = token.getAuthorizedClientRegistrationId();
        OAuth2User principal = token.getPrincipal();
        String providerUserId = principal.getName();
        String email = extractEmail(principal);
        ClientFingerprint fingerprint = ClientInfo.fingerprint(request);

        TokenPair pair = switch (provider.toLowerCase(java.util.Locale.ROOT)) {
            case GOOGLE -> {
                GoogleLoginResult result = googleLoginService.login(
                        new GoogleLoginCommand(providerUserId, email, fingerprint));
                yield result.tokens();
            }
            case APPLE -> {
                AppleLoginResult result = appleLoginService.login(
                        new AppleLoginCommand(providerUserId, email, fingerprint));
                yield result.tokens();
            }
            default -> {
                log.warn("Unsupported oauth provider {}", provider);
                response.sendRedirect(props.getFailureUrl());
                yield null;
            }
        };

        if (pair == null) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        cookieWriter.apply(headers, pair);
        headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));

        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        request.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        if (log.isDebugEnabled()) {
            log.debug("OAuth login success provider={} ip={}", provider, fingerprint.ipAddress());
        }
        response.sendRedirect(props.getSuccessUrl());
    }

    private String extractEmail(OAuth2User principal) {
        Object value = principal.getAttributes().get("email");
        if (value instanceof String email && !email.isBlank()) {
            return email;
        }
        if (principal instanceof OidcUser oidc) {
            String email = oidc.getEmail();
            if (email != null && !email.isBlank()) {
                return email;
            }
        }
        Map<String, Object> contact = asMap(principal.getAttributes().get("user"));
        if (contact != null && contact.get("email") instanceof String email && !email.isBlank()) {
            return email;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }
}
