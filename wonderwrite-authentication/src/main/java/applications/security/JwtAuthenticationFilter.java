package applications.security;

import applications.extensions.exception.TokenException;
import applications.extensions.types.TokenType;
import applications.utils.tokens.JwtTokenProvider;
import applications.utils.tokens.TokenClaims;
import applications.utils.tokens.VerifyTokenCommand;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull
            HttpServletRequest request,
            @NonNull
            HttpServletResponse response,
            @NonNull
            FilterChain chain
    ) throws ServletException, IOException {
        try {
            Optional<String> maybe = extractToken(request);
            if (maybe.isPresent()) {
                authenticate(request, maybe.get());
            }
        } catch (TokenException ex) {
            log.debug("Access token rejected: {}", ex.getMessage());
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String rawToken) {
        TokenClaims claims = tokenProvider
                .verify(new VerifyTokenCommand(rawToken, TokenType.ACCESS))
                .claims();

        AuthPrincipal principal = new AuthPrincipal(
                claims.userId(),
                claims.email(),
                claims.role()
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority(
                        "ROLE_" + claims.role().name()
                ))
        );

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                return Optional.of(token);
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (AuthCookieWriter.ACCESS_COOKIE.equals(c.getName())) {
                    String v = c.getValue();
                    if (v != null && !v.isBlank()) {
                        return Optional.of(v);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
