package applications.domain.repositories;

import applications.domain.entities.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface LoginAttemptRepositories extends JpaRepository<LoginAttempt, UUID> {

    @Query("select count(a) from LoginAttempt a " +
           "where a.email = :email and a.succeeded = false and a.attemptedAt >= :since")
    long countFailedByEmailSince(@Param("email") String email, @Param("since") LocalDateTime since);

    @Query("select count(a) from LoginAttempt a " +
           "where a.ipAddress = :ip and a.attemptedAt >= :since")
    long countAttemptsByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);
}
