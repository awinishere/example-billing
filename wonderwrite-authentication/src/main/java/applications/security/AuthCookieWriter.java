package applications.security;

import applications.tokens.TokenPair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieWriter {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    public static final String REFRESH_PATH = "/api/v1/refresh";

    public void apply(HttpHeaders headers, TokenPair pair) {
        headers.add(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, pair.accessToken(), "/", pair.accessTtlSeconds()).toString());
        headers.add(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, pair.refreshToken(), REFRESH_PATH, pair.refreshTtlSeconds()).toString());
    }

    private ResponseCookie cookie(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}
