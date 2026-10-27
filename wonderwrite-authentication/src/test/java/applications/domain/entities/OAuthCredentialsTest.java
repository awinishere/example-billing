package applications.domain.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthCredentialsTest {

    @Test
    void newLinkCarriesAllIdentityFields() {
        UUID userId = UUID.randomUUID();

        OAuthCredentials link = OAuthCredentials.newLink(
                userId,
                "google",
                "110483738369414381905",
                "user@gmail.com"
        );

        assertThat(link.getId()).isNull();
        assertThat(link.getUserId()).isEqualTo(userId);
        assertThat(link.getProvider()).isEqualTo("google");
        assertThat(link.getProviderUserId()).isEqualTo("110483738369414381905");
        assertThat(link.getEmail()).isEqualTo("user@gmail.com");
    }

    @Test
    void changeEmailReplacesStoredEmail() {
        OAuthCredentials link = OAuthCredentials.newLink(
                UUID.randomUUID(),
                "apple",
                "001429.abc",
                "old@icloud.com"
        );

        link.changeEmail("new@icloud.com");

        assertThat(link.getEmail()).isEqualTo("new@icloud.com");
    }
}
