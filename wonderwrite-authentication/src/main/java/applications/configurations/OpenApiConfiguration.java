package applications.configurations;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";
    public static final String ACCESS_TOKEN_COOKIE = "accessTokenCookie";

    @Bean
    OpenAPI wonderwriteOpenApi() {
        return new OpenAPI().components(new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("RS256-signed JWT access token. Send as 'Authorization: Bearer <token>'."))
                .addSecuritySchemes(ACCESS_TOKEN_COOKIE, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("access_token")
                        .description("HttpOnly access-token cookie set by the login endpoints.")));
    }
}
