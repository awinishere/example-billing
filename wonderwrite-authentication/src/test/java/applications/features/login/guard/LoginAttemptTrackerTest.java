package applications.features.login.guard;

import applications.domain.entities.LoginAttempt;
import applications.domain.repositories.LoginAttemptRepositories;
import applications.extensions.exception.RateLimitedException;
import applications.properties.LoginPolicyProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptTrackerTest {

    @Mock
    private LoginAttemptRepositories repositories;

    @Mock
    private LoginPolicyProperties policy;

    @InjectMocks
    private LoginAttemptTracker tracker;

    @Test
    void allowsLoginWhenBelowEmailThreshold() {
        when(policy.getEmailLockout()).thenReturn(Duration.ofMinutes(15));
        when(policy.getMaxFailedPerEmail()).thenReturn(5);
        when(repositories.countFailedByEmailSince(eq("user@example.com"), any())).thenReturn(2L);

        assertThatCode(() -> tracker.assertAllowed(
                new AssertLoginAllowedCommand("user@example.com", null))).doesNotThrowAnyException();
    }

    @Test
    void blocksAfterTooManyFailedAttemptsForEmail() {
        when(policy.getEmailLockout()).thenReturn(Duration.ofMinutes(15));
        when(policy.getMaxFailedPerEmail()).thenReturn(5);
        when(repositories.countFailedByEmailSince(eq("user@example.com"), any())).thenReturn(5L);

        assertThatThrownBy(() -> tracker.assertAllowed(new AssertLoginAllowedCommand("user@example.com", null)))
                .isInstanceOf(RateLimitedException.class)
                .hasMessage("Too many failed login attempts");
    }

    @Test
    void blocksWhenIpExceedsWindow() {
        when(policy.getEmailLockout()).thenReturn(Duration.ofMinutes(15));
        when(policy.getMaxFailedPerEmail()).thenReturn(5);
        when(repositories.countFailedByEmailSince(eq("user@example.com"), any())).thenReturn(0L);
        when(policy.getIpRateWindow()).thenReturn(Duration.ofMinutes(1));
        when(policy.getMaxAttemptsPerIp()).thenReturn(20);
        when(repositories.countAttemptsByIpSince(eq("203.0.113.7"), any())).thenReturn(20L);

        assertThatThrownBy(() -> tracker.assertAllowed(new AssertLoginAllowedCommand("user@example.com", "203.0.113.7")))
                .isInstanceOf(RateLimitedException.class)
                .hasMessage("Too many requests from this IP");
    }

    @Test
    void skipsIpCheckWhenIpBlank() {
        when(policy.getEmailLockout()).thenReturn(Duration.ofMinutes(15));
        when(policy.getMaxFailedPerEmail()).thenReturn(5);
        when(repositories.countFailedByEmailSince(eq("user@example.com"), any())).thenReturn(0L);

        tracker.assertAllowed(new AssertLoginAllowedCommand("user@example.com", "  "));

        verify(repositories, org.mockito.Mockito.never()).countAttemptsByIpSince(anyString(), any());
    }

    @Test
    void recordPersistsAttempt() {
        tracker.record(new RecordLoginAttemptCommand("user@example.com", "203.0.113.7", false));

        ArgumentCaptor<LoginAttempt> saved = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(repositories).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getValue().getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(saved.getValue().isSucceeded()).isFalse();
    }
}
