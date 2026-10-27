package applications.features.logout;

import applications.utils.tokens.RefreshTokenIssuer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class LogoutServiceTest {

    @Mock
    private RefreshTokenIssuer refreshTokenIssuer;

    @InjectMocks
    private LogoutService service;

    @Test
    void anonymousRequestHasNoSession() {
        LogoutResult result = service.execute(new LogoutCommand(null));

        assertThat(result).isInstanceOf(LogoutResult.Failed.class);
        assertThat(((LogoutResult.Failed) result).reason()).isEqualTo("No authenticated session");
        verify(refreshTokenIssuer, never()).revokeAllSessions(any());
    }

    @Test
    void authenticatedRequestRevokesAllSessions() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenIssuer.revokeAllSessions(userId)).thenReturn(2);

        LogoutResult result = service.execute(new LogoutCommand(userId));

        assertThat(result).isInstanceOf(LogoutResult.Success.class);
        assertThat(((LogoutResult.Success) result).sessions()).isEqualTo(2);
    }
}
