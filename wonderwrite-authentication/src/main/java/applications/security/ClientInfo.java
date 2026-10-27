package applications.security;

import applications.domain.model.ClientFingerprint;
import jakarta.servlet.http.HttpServletRequest;

public final class ClientInfo {

    private static final String XFF_HEADER = "X-Forwarded-For";

    public static ClientFingerprint fingerprint(HttpServletRequest request) {
        return new ClientFingerprint(request.getHeader("User-Agent"), resolveIp(request));
    }

    private static String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader(XFF_HEADER);
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
