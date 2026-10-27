package applications.security;

import applications.utils.tokens.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieWriterTest {

    private final AuthCookieWriter writer = new AuthCookieWriter();

    @Test
    void applyWritesHttpOnlyCookiesWithScopedPaths() {
        HttpHeaders headers = new HttpHeaders();
        TokenPair pair = new TokenPair("access-value", "refresh-value", 900, 604800);

        writer.apply(headers, pair);

        assertThat(headers.get(HttpHeaders.SET_COOKIE)).hasSize(2);
        String access = headers.get(HttpHeaders.SET_COOKIE).get(0);
        String refresh = headers.get(HttpHeaders.SET_COOKIE).get(1);

        assertThat(access)
                .contains("access_token=access-value")
                .contains("Path=/")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                .contains("Max-Age=900");
        assertThat(refresh)
                .contains("refresh_token=refresh-value")
                .contains("Path=" + AuthCookieWriter.REFRESH_PATH)
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                .contains("Max-Age=604800");
    }

    @Test
    void clearEmptiesBothCookiesWithZeroMaxAge() {
        HttpHeaders headers = new HttpHeaders();

        writer.clear(headers);

        assertThat(headers.get(HttpHeaders.SET_COOKIE)).hasSize(2);
        String access = headers.get(HttpHeaders.SET_COOKIE).get(0);
        String refresh = headers.get(HttpHeaders.SET_COOKIE).get(1);

        assertThat(access)
                .startsWith("access_token")
                .contains("Max-Age=0")
                .contains("Path=/");
        assertThat(refresh)
                .startsWith("refresh_token")
                .contains("Max-Age=0")
                .contains("Path=" + AuthCookieWriter.REFRESH_PATH);
    }
}
