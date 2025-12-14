package ru.dimension.ui.bus;

import lombok.extern.log4j.Log4j2;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.PublicationError;

/**
 * Custom publication error handler that logs errors using Log4j2.
 */
@Log4j2
public class LoggingPublicationErrorHandler implements IPublicationErrorHandler {

  @Override
  public void handleError(PublicationError error) {
    log.error("Event bus publication error occurred");
    log.error("Handler: {}", error.getHandler());
    log.error("Listener: {}", error.getListener());
    log.error("Message: {}", error.getMessage());

    if (error.getCause() != null) {
      log.error("Cause: {}", error.getCause().getMessage(), error.getCause());
    }
  }
}