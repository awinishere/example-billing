package applications.configurations;

import applications.properties.OAuthProperties;
import applications.security.OAuthLoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class NetworkConfiguration {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/register",
            "/api/v1/register/google",
            "/api/v1/register/apple",
            "/api/v1/login",
            "/api/v1/login/google",
            "/api/v1/login/apple",
            "/api/v1/login/verify-otp",
            "/api/v1/forgot-password",
            "/api/v1/reset-password",
            "/v3/api-docs/**",
            "/docs",
            "/docs/**",
            "/scalar",
            "/scalar/**",
            "/error"
    };

    private static final String[] OAUTH_ENDPOINTS = {
            "/oauth2/authorization/**",
            "/login/oauth2/code/**"
    };

    private final ObjectMapper objectMapper;
    private final OAuthProperties oauthProperties;
    private final OAuthLoginSuccessHandler oauthSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public NetworkConfiguration(
            ObjectMapper objectMapper,
            OAuthProperties oauthProperties,
            OAuthLoginSuccessHandler oauthSuccessHandler,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider
    ) {
        this.objectMapper = objectMapper;
        this.oauthProperties = oauthProperties;
        this.oauthSuccessHandler = oauthSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepositoryProvider.getIfAvailable();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' 'unsafe-inline'; "
                                        + "style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; "
                                        + "connect-src 'self'; "
                                        + "font-src 'self' data:; "
                                        + "object-src 'none'; "
                                        + "base-uri 'self'; "
                                        + "frame-ancestors 'none'; "
                                        + "form-action 'self'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000))
                )
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();
                    if (clientRegistrationRepository != null) {
                        auth.requestMatchers(OAUTH_ENDPOINTS).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((
                                req,
                                res,
                                e
                        ) -> writeError(
                                res,
                                HttpStatus.UNAUTHORIZED,
                                "UNAUTHENTICATED",
                                "Authentication required"
                        ))
                        .accessDeniedHandler((
                                req,
                                res,
                                e
                        ) -> writeError(
                                res,
                                HttpStatus.FORBIDDEN,
                                "ACCESS_DENIED",
                                "Insufficient privileges"
                        ))
                );

        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(oauthSuccessHandler)
                    .failureHandler((
                            req,
                            res,
                            e
                    ) -> res.sendRedirect(oauthProperties.getFailureUrl()))
            );
        }

        return http.build();
    }

    private void writeError(
            HttpServletResponse res,
            HttpStatus status,
            String reason,
            String message
    ) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(res.getWriter(), new ErrorResponse(reason, message));
    }

    private record ErrorResponse(
            String reason,
            String message
    ) {
    }
}
