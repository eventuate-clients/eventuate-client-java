package io.eventuate.javaclient.commonimpl;

public class EventTypeAndData {

  private String eventType;
  private String eventData;

  public EventTypeAndData(String eventType, String eventData) {
    this.eventType = eventType;
    this.eventData = eventData;
  }

  public String getEventType() {
    return eventType;
  }

  public String getEventData() {
    return eventData;
  }
}
