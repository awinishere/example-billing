package applications.emails;

import applications.emails.features.send.RenderTemplatesCommand;
import applications.emails.features.send.ResolveFormCommand;
import applications.emails.features.send.SendEmailCommand;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailCommandTest {

    @Test
    void commandCarriesTemplateAndModelIntoRenderCommand() {
        SendEmailCommand command = new SendEmailCommand(
                "no-reply@example.com",
                "user@example.com",
                null,
                "Verify your account",
                "otp-code",
                Map.of("code", "123456")
        );

        RenderTemplatesCommand renderCommand = command.toRenderCommand();
        ResolveFormCommand resolveFormCommand = command.toResolveFormCommand();

        assertEquals("otp-code", renderCommand.template());
        assertEquals("123456", renderCommand.model().get("code"));
        assertEquals("no-reply@example.com", resolveFormCommand.from());
    }

    @Test
    void nullModelBecomesEmptyMap() {
        SendEmailCommand command = new SendEmailCommand(
                null,
                "user@example.com",
                null,
                "Welcome",
                "welcome-new-account",
                null
        );
        RenderTemplatesCommand renderCommand = new RenderTemplatesCommand("welcome-new-account", null);

        assertTrue(command.model().isEmpty());
        assertTrue(renderCommand.model().isEmpty());
    }

    @Test
    void modelIsCopiedDefensively() {
        Map<String, Object> mutableModel = new HashMap<>();
        mutableModel.put("code", "123456");

        SendEmailCommand command = new SendEmailCommand(
                null,
                "user@example.com",
                null,
                "Welcome",
                "welcome-new-account",
                mutableModel
        );
        mutableModel.put("code", "654321");

        assertEquals("123456", command.model().get("code"));
    }

    @Test
    void commandRejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> new SendEmailCommand(null, " ", null, "Welcome", "welcome-new-account", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SendEmailCommand(null, "user@example.com", null, " ", "welcome-new-account", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SendEmailCommand(null, "user@example.com", null, "Welcome", " ", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RenderTemplatesCommand(" ", Map.of()));
    }
}
