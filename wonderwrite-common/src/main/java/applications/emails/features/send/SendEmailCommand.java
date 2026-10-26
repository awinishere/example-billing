package applications.emails.features.send;

import java.util.Collections;
import java.util.Map;

public record SendEmailCommand(
        String from,
        String to,
        String cc,
        String subject,
        String template,
        Map<String, Object> model
) {
    public SendEmailCommand {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Email recipient ('to') must not be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject must not be blank");
        }
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("Email template must not be blank");
        }
        model = model == null ? Collections.emptyMap() : Map.copyOf(model);
    }

    public RenderTemplatesCommand toRenderCommand() {
        return new RenderTemplatesCommand(template, model);
    }

    public ResolveFormCommand toResolveFormCommand() {
        return new ResolveFormCommand(from);
    }
}
