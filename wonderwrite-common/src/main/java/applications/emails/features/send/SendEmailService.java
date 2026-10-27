package applications.emails.features.send;

import applications.emails.api.SendEmailContract;
import applications.emails.extensions.exception.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
public class SendEmailService implements SendEmailContract {

    private static final String CHARSET = "UTF-8";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String defaultFrom;

    public SendEmailService(JavaMailSender mailSender,
                            TemplateEngine templateEngine,
                            @Value("${app.mail.from:}") String defaultFrom) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.defaultFrom = defaultFrom;
    }

    @Override
    public void execute(SendEmailCommand command) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Preparing email: to='{}', cc='{}', subject='{}', template='{}'",
                    command.to(),
                    command.cc(),
                    command.subject(),
                    command.template()
            );
        }

        String html = render(command.toRenderCommand());
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, CHARSET);
            helper.setFrom(resolveFrom(command.toResolveFormCommand()));
            helper.setTo(command.to());

            if (command.cc() != null && !command.cc().isBlank()) {
                helper.setCc(command.cc());
            }

            helper.setSubject(command.subject());
            helper.setText(html, true);

            mailSender.send(message);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Email sent to='{}' cc='{}' subject='{}'",
                        command.to(),
                        command.cc(),
                        command.subject()
                );
            }
        } catch (MessagingException | MailException ex) {
            log.error(
                    "Failed to deliver email to='{}' subject='{}': {}",
                    command.to(),
                    command.subject(),
                    ex.getMessage(),
                    ex
            );
            throw new EmailException(
                    "Failed to deliver email to '%s': %s".formatted(command.to(), ex.getMessage()),
                    ex
            );
        }
    }

    private String render(RenderTemplatesCommand command) {
        Context context = new Context();
        context.setVariables(command.model());

        try {
            String html = templateEngine.process(command.template(), context);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Rendered template='{}' into {} chars of HTML",
                        command.template(),
                        html.length()
                );
            }
            return html;
        } catch (Exception ex) {
            log.error(
                    "Failed to render Thymeleaf template='{}': {}",
                    command.template(),
                    ex.getMessage(),
                    ex
            );
            throw new EmailException(
                    "Failed to render Thymeleaf template '%s': %s".formatted(command.template(), ex.getMessage()),
                    ex
            );
        }
    }

    private String resolveFrom(ResolveFormCommand command) {
        if (command.from() != null && !command.from().isBlank()) {
            return command.from();
        }
        if (defaultFrom.isBlank()) {
            throw new IllegalStateException("No email sender configured, set app.mail.from");
        }
        return defaultFrom;
    }
}
