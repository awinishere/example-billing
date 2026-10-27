package applications.emails.features.send;

import java.util.Collections;
import java.util.Map;

public record RenderTemplatesCommand(
        String template,
        Map<String, Object> model
) {
    public RenderTemplatesCommand {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("Email template must not be blank");
        }
        model = model == null ? Collections.emptyMap() : Map.copyOf(model);
    }
}
