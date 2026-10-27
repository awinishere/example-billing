package applications.utils.tokens;

import applications.properties.OAuthProperties.Apple;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class AppleClientSecretGenerator {

    private static final String AUDIENCE = "https://appleid.apple.com";
    private static final Duration TTL = Duration.ofDays(150);

    public String generate(Apple props) {
        PrivateKey key = parsePrivateKey(props.getPrivateKeyPem());
        Instant now = Instant.now();
        return Jwts.builder()
                .header().add("kid", props.getKeyId()).and()
                .issuer(props.getTeamId())
                .audience().add(AUDIENCE).and()
                .subject(props.getClientId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TTL)))
                .signWith(key, Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey parsePrivateKey(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException(
                    "app.security.oauth.apple.private-key-pem is required when apple.enabled=true"
            );
        }
        byte[] der = extractDer(pem.replace("\\n", "\n"));
        try {
            return KeyFactory
                    .getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load Apple EC private key", ex);
        }
    }

    private byte[] extractDer(String pem) {
        StringBuilder body = new StringBuilder();
        boolean inBody = false;
        for (String line : pem.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-----BEGIN")) {
                inBody = true;
                continue;
            }
            if (trimmed.startsWith("-----END")) {
                inBody = false;
                continue;
            }
            if (inBody || !trimmed.startsWith("-----")) {
                body.append(trimmed);
            }
        }

        if (body.isEmpty()) {
            throw new IllegalStateException("No base64 body found in Apple private key PEM");
        }

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        sink.writeBytes(Base64.getDecoder().decode(body.toString()));
        return sink.toByteArray();
    }
}
