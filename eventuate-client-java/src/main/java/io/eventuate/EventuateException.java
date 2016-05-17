package io.eventuate;

public class EventuateException extends RuntimeException {

  public EventuateException() {
  }

  public EventuateException(String m) {
    super(m);
  }

  public EventuateException(String m, Exception e) {
    super(m, e);
  }
}
