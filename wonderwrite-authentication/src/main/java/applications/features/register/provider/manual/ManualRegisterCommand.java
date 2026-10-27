package applications.features.register.provider.manual;

import java.util.Locale;

public record ManualRegisterCommand(
        String email,
        String password,
        String confirmPassword
) {
    public ManualRegisterCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }
        email = email.trim().toLowerCase(Locale.ROOT);
    }
}
