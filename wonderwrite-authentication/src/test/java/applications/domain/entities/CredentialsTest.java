package applications.domain.entities;

import applications.extensions.types.RoleAccount;
import applications.extensions.types.StatusAccount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialsTest {

    @Test
    void newAccountStartsUnverifiedAndActive() {
        Credentials account = Credentials.newAccount(
                "user@example.com",
                "hash-value"
        );

        assertThat(account.getId()).isNull();
        assertThat(account.getEmail()).isEqualTo("user@example.com");
        assertThat(account.getPassword()).isEqualTo("hash-value");
        assertThat(account.isEmailVerification()).isFalse();
        assertThat(account.getAccountStatus()).isEqualTo(StatusAccount.ACTIVE);
        assertThat(account.getRole()).isEqualTo(RoleAccount.USER);
        assertThat(account.getAuthProvider()).isNull();
    }

    @Test
    void newOAuthAccountIsVerifiedAndCarriesProvider() {
        Credentials account = Credentials.newOAuthAccount(
                "user@gmail.com",
                "hash-value",
                "google"
        );

        assertThat(account.isEmailVerification()).isTrue();
        assertThat(account.getAccountStatus()).isEqualTo(StatusAccount.ACTIVE);
        assertThat(account.getAuthProvider()).isEqualTo("google");
    }

    @Test
    void markEmailAsVerifiedFlipsFlag() {
        Credentials account = Credentials.newAccount(
                "user@example.com",
                "hash-value"
        );

        account.markEmailAsVerified();

        assertThat(account.isEmailVerification()).isTrue();
    }

    @Test
    void changePasswordReplacesStoredHash() {
        Credentials account = Credentials.newAccount(
                "user@example.com",
                "old-hash"
        );

        account.changePassword("new-hash");

        assertThat(account.getPassword()).isEqualTo("new-hash");
    }
}
