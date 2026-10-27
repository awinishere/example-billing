package applications.emails.api;

import applications.emails.features.send.SendEmailCommand;

public interface SendEmailContract {

    void execute(SendEmailCommand command);
}
