package applications.utils.tokens;

import applications.domain.entities.RefreshSecret;
import applications.domain.repositories.RefreshSecretRepositories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenIssuer {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshSecretRepositories repositories;

    @Transactional
    public StartTokenFamilyResult startFamily(StartTokenFamilyCommand command) {
        UUID familyId = UUID.randomUUID();
        IssueTokenResult issued = jwtTokenProvider.issue(new IssueTokenCommand(
                command.userId(),
                command.email(),
                command.role()
                )
        );
        TokenPair pair = issued.tokens();

        repositories.save(RefreshSecret.newSecret(
                command.userId(),
                familyId,
                sha256Hex(pair.refreshToken()),
                LocalDateTime.now().plusSeconds(pair.refreshTtlSeconds()),
                command.fingerprint()
        ));

        if (log.isDebugEnabled()) {
            log.debug("Token family started family={} userId={}", familyId, command.userId());
        }
        return new StartTokenFamilyResult(pair);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
