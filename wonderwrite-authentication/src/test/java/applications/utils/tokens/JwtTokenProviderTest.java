package applications.utils.tokens;

import applications.extensions.exception.TokenException;
import applications.extensions.types.RoleAccount;
import applications.extensions.types.TokenType;
import applications.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    @TempDir
    Path tempDir;

    private JwtTokenProvider provider;
    private JwtProperties props;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        props = new JwtProperties();
        writeKeyPairFiles(tempDir, "primary");
        props.setPrivateKeyPath(tempDir.resolve("primary-private.pem"));
        props.setPublicKeyPath(tempDir.resolve("primary-public.pem"));
        props.setIssuer("wonderwrite-test");
        props.setAccessTokenTtl(Duration.ofMinutes(15));
        props.setRefreshTokenTtl(Duration.ofDays(7));
        userId = UUID.randomUUID();

        provider = new JwtTokenProvider(props);
        provider.init();
    }

    @Test
    void issueReturnsPairWithConfiguredTtlSeconds() {
        IssueTokenResult issued = provider.issue(new IssueTokenCommand(userId, "user@example.com", RoleAccount.USER));

        assertThat(issued.tokens().accessToken()).isNotBlank();
        assertThat(issued.tokens().refreshToken()).isNotBlank();
        assertThat(issued.tokens().accessToken()).isNotEqualTo(issued.tokens().refreshToken());
        assertThat(issued.tokens().accessTtlSeconds()).isEqualTo(900);
        assertThat(issued.tokens().refreshTtlSeconds()).isEqualTo(604800);
    }

    @Test
    void issuedAccessTokenVerifiesBackToOriginalClaims() {
        TokenPair pair = provider.issue(new IssueTokenCommand(userId, "user@example.com", RoleAccount.ADMIN))
                .tokens();

        TokenClaims claims = provider.verify(new VerifyTokenCommand(pair.accessToken(), TokenType.ACCESS)).claims();

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.role()).isEqualTo(RoleAccount.ADMIN);
        assertThat(claims.type()).isEqualTo(TokenType.ACCESS);
        assertThat(claims.expiresAtEpochSeconds()).isGreaterThan(claims.issuedAtEpochSeconds());
    }

    @Test
    void issuedRefreshTokenVerifiesAsRefreshType() {
        TokenPair pair = provider.issue(new IssueTokenCommand(userId, "user@example.com", RoleAccount.USER))
                .tokens();

        TokenClaims claims = provider.verify(new VerifyTokenCommand(pair.refreshToken(), TokenType.REFRESH)).claims();

        assertThat(claims.type()).isEqualTo(TokenType.REFRESH);
        assertThat(claims.userId()).isEqualTo(userId);
    }

    @Test
    void verifyingAccessTokenWithExpectedRefreshTypeIsRejected() {
        TokenPair pair = provider.issue(new IssueTokenCommand(userId, "user@example.com", RoleAccount.USER))
                .tokens();

        assertThatThrownBy(() -> provider.verify(new VerifyTokenCommand(pair.accessToken(), TokenType.REFRESH)))
                .isInstanceOf(TokenException.class)
                .hasMessage("Wrong token type, expected REFRESH");
    }

    @Test
    void expiredTokenIsRejected() {
        props.setAccessTokenTtl(Duration.ofMinutes(-5));

        TokenPair pair = provider.issue(new IssueTokenCommand(userId, "user@example.com", RoleAccount.USER))
                .tokens();

        assertThatThrownBy(() -> provider.verify(new VerifyTokenCommand(pair.accessToken(), TokenType.ACCESS)))
                .isInstanceOf(TokenException.class)
                .hasMessage("Token has expired");
    }

    @Test
    void garbageTokenIsRejected() {
        assertThatThrownBy(() -> provider.verify(new VerifyTokenCommand("not-a-jwt", TokenType.ACCESS)))
                .isInstanceOf(TokenException.class)
                .hasMessage("Invalid token");
    }

    @Test
    void tokenSignedByAnotherKeyPairIsRejected() throws Exception {
        Path otherDir = Files.createDirectories(tempDir.resolve("other"));
        writeKeyPairFiles(otherDir, "foreign");
        JwtProperties foreign = new JwtProperties();
        foreign.setPrivateKeyPath(otherDir.resolve("foreign-private.pem"));
        foreign.setPublicKeyPath(otherDir.resolve("foreign-public.pem"));
        foreign.setIssuer("wonderwrite-test");
        foreign.setAccessTokenTtl(Duration.ofMinutes(15));
        foreign.setRefreshTokenTtl(Duration.ofDays(7));

        JwtTokenProvider foreignProvider = new JwtTokenProvider(foreign);
        foreignProvider.init();
        TokenPair forged = foreignProvider.issue(new IssueTokenCommand(userId, "user@example.com", RoleAccount.USER))
                .tokens();

        assertThatThrownBy(() -> provider.verify(new VerifyTokenCommand(forged.accessToken(), TokenType.ACCESS)))
                .isInstanceOf(TokenException.class)
                .hasMessage("Invalid token");
    }

    private void writeKeyPairFiles(Path dir, String name) throws IOException, NoSuchAlgorithmException {
        KeyPair keyPair = generateRsa();
        Files.writeString(dir.resolve(name + "-private.pem"), toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        Files.writeString(dir.resolve(name + "-public.pem"), toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
    }

    private KeyPair generateRsa() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String toPem(String marker, byte[] encoded) {
        return "-----BEGIN " + marker + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded)
                + "\n-----END " + marker + "-----\n";
    }
}
