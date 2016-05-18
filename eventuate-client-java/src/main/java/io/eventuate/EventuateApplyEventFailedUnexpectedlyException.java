package io.eventuate;

public class EventuateApplyEventFailedUnexpectedlyException extends EventuateClientException {

  public EventuateApplyEventFailedUnexpectedlyException(ReflectiveOperationException e) {
    super(e);
  }

}
