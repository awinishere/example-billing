package applications.domain.repositories;

import applications.domain.entities.OneTimePassCode;
import applications.extensions.types.OneTimePassPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OneTimePassCodeRepositories extends JpaRepository<OneTimePassCode, UUID> {

    Optional<OneTimePassCode> findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
            UUID userId,
            OneTimePassPurpose purpose
    );
}
