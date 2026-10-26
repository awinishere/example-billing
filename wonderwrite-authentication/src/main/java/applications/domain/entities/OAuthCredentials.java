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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Table(schema = "private_identity", name = "oauth_credentials")
public class OAuthCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false, length = 32)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, updatable = false, length = 255)
    private String providerUserId;

    @Column(nullable = false, length = 255)
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static OAuthCredentials newLink(UUID userId, String provider, String providerUserId, String email) {
        OAuthCredentials credentials = new OAuthCredentials();
        credentials.userId = userId;
        credentials.provider = provider;
        credentials.providerUserId = providerUserId;
        credentials.email = email;
        return credentials;
    }

    public void changeEmail(String email) {
        this.email = email;
    }
}
