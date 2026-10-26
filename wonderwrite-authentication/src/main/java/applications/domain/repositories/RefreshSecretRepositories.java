package applications.domain.repositories;

import applications.domain.entities.RefreshSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RefreshSecretRepositories extends JpaRepository<RefreshSecret, UUID> {
}
