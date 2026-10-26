package applications.domain.entities;

import applications.extensions.types.RoleAccount;
import applications.extensions.types.StatusAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Table(schema = "private_identity", name = "credentials")
public class Credentials {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private String password;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private RoleAccount role = RoleAccount.USER;

    @Column(name = "email_verification", nullable = false)
    private boolean emailVerification = false;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 16)
    private StatusAccount accountStatus = StatusAccount.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Credentials newAccount(String email, String passwordHash) {
        Credentials credentials = new Credentials();
        credentials.email = email;
        credentials.password = passwordHash;
        credentials.emailVerification = false;
        credentials.accountStatus = StatusAccount.ACTIVE;
        return credentials;
    }

    public void markEmailAsVerified() {
        this.emailVerification = true;
    }

    public void changePassword(String passwordHash) {
        this.password = passwordHash;
    }
}
