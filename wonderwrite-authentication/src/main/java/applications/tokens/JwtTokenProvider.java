package applications.tokens;

import applications.configurations.JwtProperties;
import applications.extensions.exception.TokenException;
import applications.extensions.types.RoleAccount;
import applications.extensions.types.TokenType;
import applications.tokens.command.BuildTokenCommand;
import applications.tokens.command.IssueTokenCommand;
import applications.tokens.command.VerifyTokenCommand;
import applications.tokens.result.IssueTokenResult;
import applications.tokens.result.VerifyTokenResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";

    private final JwtProperties props;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        this.signingKey = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        if (log.isDebugEnabled()) {
            log.debug("JwtTokenProvider ready issuer={} accessTtl={} refreshTtl={}",
                    props.getIssuer(), props.getAccessTokenTtl(), props.getRefreshTokenTtl());
        }
    }

    public IssueTokenResult issue(IssueTokenCommand command) {
        Instant now = Instant.now();
        TokenPair pair = new TokenPair(
                build(new BuildTokenCommand(command.userId(), command.email(), command.role(), TokenType.ACCESS, now, props.getAccessTokenTtl())),
                build(new BuildTokenCommand(command.userId(), command.email(), command.role(), TokenType.REFRESH, now, props.getRefreshTokenTtl())),
                props.getAccessTokenTtl().toSeconds(),
                props.getRefreshTokenTtl().toSeconds()
        );
        return new IssueTokenResult(pair);
    }

    public VerifyTokenResult verify(VerifyTokenCommand command) {
        Claims payload;
        try {
            payload = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(props.getIssuer())
                    .build()
                    .parseSignedClaims(command.token())
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new TokenException("Token has expired", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenException("Invalid token", ex);
        }

        String typeClaim = payload.get(CLAIM_TYPE, String.class);
        if (typeClaim == null || !typeClaim.equals(command.expected().name())) {
            throw new TokenException("Wrong token type, expected " + command.expected());
        }

        try {
            return new VerifyTokenResult(new TokenClaims(
                    UUID.fromString(payload.getSubject()),
                    payload.get(CLAIM_EMAIL, String.class),
                    RoleAccount.valueOf(payload.get(CLAIM_ROLE, String.class)),
                    command.expected(),
                    payload.getIssuedAt().toInstant().getEpochSecond(),
                    payload.getExpiration().toInstant().getEpochSecond()
            ));
        } catch (IllegalArgumentException ex) {
            throw new TokenException("Malformed token claims", ex);
        }
    }

    private String build(BuildTokenCommand command) {
        Instant issuedAt = command.issuedAt();
        Duration ttl = command.ttl();
        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(command.userId().toString())
                .claim(CLAIM_EMAIL, command.email())
                .claim(CLAIM_ROLE, command.role().name())
                .claim(CLAIM_TYPE, command.type().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(ttl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
