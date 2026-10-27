package applications.crypto;

import applications.crypto.configurations.Argon2Configuration;
import applications.crypto.features.hashing.HashingCommand;
import applications.crypto.features.hashing.HashingResult;
import applications.crypto.features.hashing.HashingService;
import applications.crypto.features.matching.MatchingCommand;
import applications.crypto.features.matching.MatchingResult;
import applications.crypto.features.matching.MatchingService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Argon2PasswordTest {

    private final PasswordEncoder passwordEncoder = new Argon2Configuration().passwordEncoder();
    private final HashingService hashingService = new HashingService(passwordEncoder);
    private final MatchingService matchingService = new MatchingService(passwordEncoder);

    @Test
    void hashProducesArgon2EncodedValue() {
        HashingResult result = hashingService.hash(new HashingCommand("s3cr3t-pass"));

        assertTrue(result.hash().startsWith("$argon2id$"));
        assertNotEquals("s3cr3t-pass", result.hash());
    }

    @Test
    void hashOfSameInputProducesDifferentSaltedValues() {
        HashingResult first = hashingService.hash(new HashingCommand("s3cr3t-pass"));
        HashingResult second = hashingService.hash(new HashingCommand("s3cr3t-pass"));

        assertNotEquals(first.hash(), second.hash());
        assertTrue(matchingService.matches(new MatchingCommand("s3cr3t-pass", first.hash())).matched());
        assertTrue(matchingService.matches(new MatchingCommand("s3cr3t-pass", second.hash())).matched());
    }

    @Test
    void matchesRejectsWrongPlainText() {
        HashingResult result = hashingService.hash(new HashingCommand("s3cr3t-pass"));

        MatchingResult matched = matchingService.matches(new MatchingCommand("wrong-pass", result.hash()));

        assertFalse(matched.matched());
    }

    @Test
    void matchesRejectsMalformedHash() {
        MatchingResult matched = matchingService.matches(new MatchingCommand("s3cr3t-pass", "plain-text-not-a-hash"));

        assertFalse(matched.matched());
    }

    @Test
    void commandsRejectMissingInput() {
        assertThrows(IllegalArgumentException.class, () -> new HashingCommand(null));
        assertThrows(IllegalArgumentException.class, () -> new HashingCommand(""));
        assertThrows(IllegalArgumentException.class, () -> new MatchingCommand(null, "some-hash"));
        assertThrows(IllegalArgumentException.class, () -> new MatchingCommand("s3cr3t-pass", " "));
    }

    @Test
    void resultRejectsBlankHash() {
        assertThrows(IllegalArgumentException.class, () -> new HashingResult(" "));
        assertEquals("$argon2id$v=19$m=16384,t=2,p=1$x$y", new HashingResult("$argon2id$v=19$m=16384,t=2,p=1$x$y").hash());
    }
}
