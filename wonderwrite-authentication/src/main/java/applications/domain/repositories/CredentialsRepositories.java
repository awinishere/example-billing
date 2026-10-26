package applications.domain.repositories;

import applications.domain.entities.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CredentialsRepositories extends JpaRepository<Credentials, UUID> {

    Optional<Credentials> findByEmail(String email);
    boolean existsByEmail(String email);
}
