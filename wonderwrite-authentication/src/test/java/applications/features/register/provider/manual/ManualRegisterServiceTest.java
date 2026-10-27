package applications.features.register.provider.manual;

import applications.crypto.api.HashingContract;
import applications.crypto.features.hashing.HashingCommand;
import applications.crypto.features.hashing.HashingResult;
import applications.domain.entities.Credentials;
import applications.domain.entities.TestEntities;
import applications.domain.repositories.CredentialsRepositories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualRegisterServiceTest {

    @Mock
    private CredentialsRepositories repositories;

    @Mock
    private HashingContract hashingContract;

    @InjectMocks
    private ManualRegisterService service;

    private final UUID newId = UUID.randomUUID();

    @Test
    void rejectsAlreadyRegisteredEmail() {
        ManualRegisterCommand command = new ManualRegisterCommand(
                "user@example.com",
                "S3cret!passphrase",
                "S3cret!passphrase"
        );
        when(repositories.existsByEmail("user@example.com")).thenReturn(true);

        ManualRegisterResult result = service.execute(command);

        assertThat(result).isInstanceOf(ManualRegisterResult.Failed.class);
        assertThat(((ManualRegisterResult.Failed) result).reason()).isEqualTo("Email already registered");
        verify(repositories, never()).save(any());
    }

    @Test
    void hashesPasswordAndStoresAccount() {
        ManualRegisterCommand command = new ManualRegisterCommand(
                "user@example.com",
                "S3cret!passphrase",
                "S3cret!passphrase"
        );
        when(repositories.existsByEmail("user@example.com"))
                .thenReturn(false);

        when(hashingContract.hash(new HashingCommand("S3cret!passphrase")))
                .thenReturn(new HashingResult("$argon2id$stub-hash"));

        when(repositories.save(any(Credentials.class)))
                .thenAnswer(invocation -> {
            Credentials entity = invocation.getArgument(0);
            return TestEntities.withId(entity, newId);
        });

        ManualRegisterResult result = service.execute(command);

        ArgumentCaptor<Credentials> saved = ArgumentCaptor.forClass(Credentials.class);
        verify(repositories).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("$argon2id$stub-hash");
        assertThat(saved.getValue().getEmail()).isEqualTo("user@example.com");

        assertThat(result).isInstanceOf(ManualRegisterResult.Success.class);
        ManualRegisterResult.Success success = (ManualRegisterResult.Success) result;
        assertThat(success.id()).isEqualTo(newId);
        assertThat(success.email()).isEqualTo("user@example.com");
        assertThat(success.emailVerification()).isFalse();
    }
}
