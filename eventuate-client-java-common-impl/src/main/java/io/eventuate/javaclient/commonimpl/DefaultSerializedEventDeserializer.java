package io.eventuate.javaclient.commonimpl;

import io.eventuate.DispatchedEvent;
import io.eventuate.EndOfCurrentEventsReachedEvent;
import io.eventuate.Event;

import java.util.Optional;

public class DefaultSerializedEventDeserializer implements SerializedEventDeserializer {

  @Override
  public Optional<DispatchedEvent<Event>> toDispatchedEvent(SerializedEvent se) {
    String eventType = se.getEventType();
    Class<Event> eventClass = toEventClass(eventType);

    Event event = JSonMapper.fromJson(se.getEventData(), eventClass);
    return Optional.of(new DispatchedEvent<>(se.getEntityId(),
            se.getId(),
            event,
            se.getSwimLane(),
            se.getOffset(), se.getEventContext()));
  }

  private Class<Event> toEventClass(String eventType) {
    if ("net.chrisrichardson.eventstore.subscriptions.EndOfCurrentEventsReachedEvent".equals(eventType)) {
      eventType = EndOfCurrentEventsReachedEvent.class.getName();
    }
    try {
      return (Class<Event>) Class.forName(eventType);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
