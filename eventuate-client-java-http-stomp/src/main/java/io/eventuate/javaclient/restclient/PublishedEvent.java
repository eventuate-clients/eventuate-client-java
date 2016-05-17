package io.eventuate.javaclient.restclient;

import io.eventuate.EntityIdAndType;
import io.eventuate.EventIdAndType;

public class PublishedEvent {

  private EventIdAndType eventIdAndType;
  private EntityIdAndType sender;

  public PublishedEvent(EventIdAndType eventIdAndType, EntityIdAndType sender) {
    this.eventIdAndType = eventIdAndType;
    this.sender = sender;
  }

  public EventIdAndType getEventIdAndType() {
    return eventIdAndType;
  }

  public EntityIdAndType getSender() {
    return sender;
  }
}
