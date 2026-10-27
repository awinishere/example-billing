package applications.domain.entities;

import applications.domain.model.ClientFingerprint;
import applications.extensions.types.OneTimePassPurpose;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

public final class TestEntities {

    private TestEntities() {
    }

    public static Credentials credentials(UUID id, String email, String passwordHash) {
        Credentials entity = Credentials.newAccount(email, passwordHash);
        set(entity, "id", id);
        return entity;
    }

    public static Credentials verifiedCredentials(UUID id, String email, String passwordHash) {
        Credentials entity = credentials(id, email, passwordHash);
        entity.markEmailAsVerified();
        return entity;
    }

    public static OAuthCredentials oauthLink(
            UUID id,
            UUID userId,
            String provider,
            String providerUserId,
            String email
    ) {
        OAuthCredentials entity = OAuthCredentials.newLink(userId, provider, providerUserId, email);
        set(entity, "id", id);
        return entity;
    }

    public static OneTimePassCode otpCode(
            UUID id,
            UUID userId,
            OneTimePassPurpose purpose,
            String codeHash,
            LocalDateTime expiresAt,
            LocalDateTime createdAt
    ) {
        OneTimePassCode entity = OneTimePassCode.newCode(userId, purpose, codeHash, expiresAt);
        set(entity, "id", id);
        set(entity, "createdAt", createdAt);
        return entity;
    }

    public static RefreshSecret refreshSecret(
            UUID id,
            UUID userId,
            UUID familyId,
            String secretHash,
            LocalDateTime expiresAt,
            ClientFingerprint fingerprint
    ) {
        RefreshSecret entity = RefreshSecret.newSecret(
                userId,
                familyId,
                secretHash,
                expiresAt,
                fingerprint
        );
        set(entity, "id", id);
        return entity;
    }

    public static LoginAttempt loginAttempt(String email, String ipAddress, boolean succeeded, LocalDateTime attemptedAt) {
        LoginAttempt entity = LoginAttempt.newAttempt(email, ipAddress, succeeded);
        set(entity, "attemptedAt", attemptedAt);
        return entity;
    }

    public static Credentials withId(Credentials target, UUID id) {
        set(target, "id", id);
        return target;
    }

    private static void set(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
