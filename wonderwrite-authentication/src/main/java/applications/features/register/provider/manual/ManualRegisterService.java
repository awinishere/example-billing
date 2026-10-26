package applications.features.register.provider.manual;

import applications.crypto.api.HashingContract;
import applications.crypto.features.hashing.HashingCommand;
import applications.crypto.features.hashing.HashingResult;
import applications.domain.entities.Credentials;
import applications.domain.repositories.CredentialsRepositories;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class ManualRegisterService {

    private final CredentialsRepositories repositories;
    private final HashingContract hashingContract;

    @Transactional
    public ManualRegisterResult execute(ManualRegisterCommand command) {
        if (repositories.existsByEmail(command.email())) {
            if (log.isDebugEnabled()) {
                log.debug("Register rejected: email already registered");
            }
            return new ManualRegisterResult.Failed("Email already registered");
        }

        HashingResult hashed = hashingContract.hash(new HashingCommand(command.password()));
        Credentials saved = repositories.save(
                Credentials.newAccount(
                        command.email(),
                        hashed.hash()
                )
        );

        if (log.isDebugEnabled()) {
            log.debug("Account registered: id={}, passwordHashed=true", saved.getId());
        }

        return new ManualRegisterResult.Success(
                saved.getId(),
                saved.getEmail(),
                saved.isEmailVerification()
        );
    }
}
