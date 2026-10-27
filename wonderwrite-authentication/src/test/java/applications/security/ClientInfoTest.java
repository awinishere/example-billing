package applications.security;

import applications.domain.model.ClientFingerprint;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientInfoTest {

    @Test
    void fingerprintUsesRemoteAddrWithoutProxyHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64)");
        request.setRemoteAddr("203.0.113.10");

        ClientFingerprint fingerprint = ClientInfo.fingerprint(request);

        assertThat(fingerprint.userAgent()).isEqualTo("Mozilla/5.0 (X11; Linux x86_64)");
        assertThat(fingerprint.ipAddress()).isEqualTo("203.0.113.10");
    }

    @Test
    void forwardedForChainResolvesToFirstClientIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "curl/8.9.1");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 203.0.113.10");
        request.setRemoteAddr("10.0.0.1");

        ClientFingerprint fingerprint = ClientInfo.fingerprint(request);

        assertThat(fingerprint.ipAddress()).isEqualTo("198.51.100.7");
        assertThat(fingerprint.userAgent()).isEqualTo("curl/8.9.1");
    }

    @Test
    void singleValueForwardedForIsUsedAsIs() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.7");
        request.setRemoteAddr("10.0.0.1");

        assertThat(ClientInfo.fingerprint(request).ipAddress()).isEqualTo("198.51.100.7");
    }

    @Test
    void blankForwardedForFallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "   ");
        request.setRemoteAddr("10.0.0.1");

        assertThat(ClientInfo.fingerprint(request).ipAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    void missingUserAgentYieldsNullUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        ClientFingerprint fingerprint = ClientInfo.fingerprint(request);

        assertThat(fingerprint.userAgent()).isNull();
        assertThat(fingerprint.ipAddress()).isEqualTo("10.0.0.1");
    }
}
