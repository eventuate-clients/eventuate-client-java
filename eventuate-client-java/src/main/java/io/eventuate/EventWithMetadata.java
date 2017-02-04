package io.eventuate;

/**
 * An event with it's id
 */
public class EventWithMetadata {

  private Event event;
  private Int128 id;

  public EventWithMetadata(Event event, Int128 id) {
    this.event = event;
    this.id = id;
  }

  public Event getEvent() {
    return event;
  }

  public Int128 getId() {
    return id;
  }
}
