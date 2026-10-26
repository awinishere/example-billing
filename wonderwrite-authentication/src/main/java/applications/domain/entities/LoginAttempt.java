package applications.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Table(schema = "private_identity", name = "login_attempt")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 320)
    private String email;

    @Column(name = "ip_address", updatable = false, length = 64)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private boolean succeeded;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    public static LoginAttempt newAttempt(String email, String ipAddress, boolean succeeded) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.email = email;
        attempt.ipAddress = ipAddress;
        attempt.succeeded = succeeded;
        attempt.attemptedAt = LocalDateTime.now();
        return attempt;
    }
}
