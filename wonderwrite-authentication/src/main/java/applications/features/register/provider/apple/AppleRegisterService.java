package applications.features.register.provider.apple;

import applications.crypto.api.HashingContract;
import applications.crypto.features.hashing.HashingCommand;
import applications.crypto.features.hashing.HashingResult;
import applications.domain.entities.Credentials;
import applications.domain.entities.OAuthCredentials;
import applications.domain.repositories.CredentialsRepositories;
import applications.domain.repositories.OAuthCredentialsRepositories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleRegisterService {

    private static final String PROVIDER = "apple";
    private static final int PASSWORD_BYTES = 32;

    private final OAuthCredentialsRepositories oauthRepositories;
    private final CredentialsRepositories credentialsRepositories;
    private final HashingContract hashingContract;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AppleRegisterResult register(AppleRegisterCommand command) {
        Optional<OAuthCredentials> existing = oauthRepositories
                .findByProviderAndProviderUserId(PROVIDER, command.providerUserId());

        if (existing.isPresent()) {
            Credentials linked = credentialsRepositories
                    .findById(existing.get().getUserId())
                    .orElseThrow(() -> new IllegalStateException(
                            "OAuth link points at missing account " + existing.get().getUserId()
                    ));
            return toResult(linked, false);
        }

        return provision(command.providerUserId(), command.email());
    }

    private AppleRegisterResult provision(String providerUserId, String email) {
        String resolvedEmail = resolveEmail(email, providerUserId);
        Credentials account = createAccount(resolvedEmail);

        OAuthCredentials link = OAuthCredentials.newLink(
                account.getId(),
                PROVIDER,
                providerUserId,
                resolvedEmail
        );

        try {
            oauthRepositories.save(link);
        } catch (DataIntegrityViolationException raced) {
            OAuthCredentials winner = oauthRepositories
                    .findByProviderAndProviderUserId(PROVIDER, providerUserId)
                    .orElseThrow(() -> raced);

            Credentials racedAccount = credentialsRepositories
                    .findById(winner.getUserId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Race resolved but account is missing " + winner.getUserId()
                    ));

            return toResult(racedAccount, false);
        }

        if (log.isDebugEnabled()) {
            log.debug("Apple account provisioned userId={}", account.getId());
        }
        return toResult(account, true);
    }

    private Credentials createAccount(String email) {
        Credentials account = Credentials.newOAuthAccount(
                email,
                hash(generatePassword()),
                PROVIDER
        );

        try {
            return credentialsRepositories.save(account);
        } catch (DataIntegrityViolationException dupEmail) {
            if (log.isDebugEnabled()) {
                log.debug("Apple first-login matched existing email");
            }
            return credentialsRepositories
                    .findByEmail(email)
                    .orElseThrow(() -> dupEmail);
        }
    }

    private String generatePassword() {
        byte[] buffer = new byte[PASSWORD_BYTES];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String hash(String rawPassword) {
        HashingResult hashed = hashingContract.hash(new HashingCommand(rawPassword));
        return hashed.hash();
    }

    private String resolveEmail(String email, String providerUserId) {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return PROVIDER + "." + providerUserId + "@oauth.invalid";
    }

    private AppleRegisterResult toResult(Credentials account, boolean provisioned) {
        return new AppleRegisterResult(
                account.getId(),
                account.getEmail(),
                account.getRole(),
                provisioned
        );
    }
}
