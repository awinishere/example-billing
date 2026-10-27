package applications.features.register.provider.google;

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
public class GoogleRegisterService {

    private static final String PROVIDER = "google";
    private static final int PASSWORD_BYTES = 32;

    private final OAuthCredentialsRepositories oauthRepositories;
    private final CredentialsRepositories credentialsRepositories;
    private final HashingContract hashingContract;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public GoogleRegisterResult register(GoogleRegisterCommand command) {
        Optional<OAuthCredentials> existing = oauthRepositories
                .findByProviderAndProviderUserId(PROVIDER, command.providerUserId());

        if (existing.isPresent()) {
            return credentialsRepositories.findById(existing.get().getUserId())
                    .map(account -> success(account, false))
                    .orElseGet(() -> new GoogleRegisterResult.Failed(
                            "OAuth link points at missing account " + existing.get().getUserId()));
        }

        return provision(command.providerUserId(), command.email());
    }

    private GoogleRegisterResult provision(String providerUserId, String email) {
        String resolvedEmail = resolveEmail(email, providerUserId);
        Optional<Credentials> account = createAccount(resolvedEmail);

        if (account.isEmpty()) {
            return new GoogleRegisterResult.Failed("Unable to resolve account for the provided email");
        }

        Credentials created = account.get();
        OAuthCredentials link = OAuthCredentials.newLink(
                created.getId(),
                PROVIDER,
                providerUserId,
                resolvedEmail
        );

        try {
            oauthRepositories.save(link);
        } catch (DataIntegrityViolationException raced) {
            return oauthRepositories.findByProviderAndProviderUserId(PROVIDER, providerUserId)
                    .flatMap(winner -> credentialsRepositories.findById(winner.getUserId()))
                    .map(racedAccount -> success(racedAccount, false))
                    .orElseGet(() -> new GoogleRegisterResult.Failed(
                            "Race resolved but account is missing for subject " + providerUserId));
        }

        if (log.isDebugEnabled()) {
            log.debug("Google account provisioned userId={}", created.getId());
        }
        return success(created, true);
    }

    private Optional<Credentials> createAccount(String email) {
        Credentials account = Credentials.newOAuthAccount(
                email,
                hash(generatePassword()),
                PROVIDER
        );

        try {
            return Optional.of(credentialsRepositories.save(account));
        } catch (DataIntegrityViolationException dupEmail) {
            if (log.isDebugEnabled()) {
                log.debug("Google first-login matched existing email");
            }
            return credentialsRepositories.findByEmail(email);
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

    private GoogleRegisterResult success(Credentials account, boolean provisioned) {
        return new GoogleRegisterResult.Success(
                account.getId(),
                account.getEmail(),
                account.getRole(),
                provisioned
        );
    }
}
