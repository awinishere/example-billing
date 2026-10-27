package applications.features.logout;

import java.util.UUID;

public record LogoutCommand(
        UUID userId
) {
}
