package io.eventuate;

public class EventuateCommandProcessingFailedUnexpectedlyException extends EventuateClientException {
  public EventuateCommandProcessingFailedUnexpectedlyException(ReflectiveOperationException t) {
    super(t);
  }
}
