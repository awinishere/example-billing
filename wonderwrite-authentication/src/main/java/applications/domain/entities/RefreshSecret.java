package applications.domain.entities;

import applications.domain.model.ClientFingerprint;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Table(schema = "private_identity", name = "refresh_secret")
public class RefreshSecret {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "secret_hash", nullable = false, updatable = false, length = 512)
    private String secretHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static RefreshSecret newSecret(UUID userId, UUID familyId, String secretHash, LocalDateTime expiresAt, ClientFingerprint fingerprint) {
        ClientFingerprint client = fingerprint == null ? ClientFingerprint.unknown() : fingerprint;
        RefreshSecret secret = new RefreshSecret();
        secret.userId = userId;
        secret.familyId = familyId;
        secret.secretHash = secretHash;
        secret.expiresAt = expiresAt;
        secret.userAgent = client.userAgent();
        secret.ipAddress = client.ipAddress();
        return secret;
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(this.expiresAt);
    }
}
