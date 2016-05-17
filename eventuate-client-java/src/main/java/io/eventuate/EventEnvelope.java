package io.eventuate;

public interface EventEnvelope<T extends Event> {
  Int128 getEventId();

  Class<?> getEventType();

  String getEntityId();

  Integer getSwimlane();

  Long getOffset();

  EventContext getEventContext();
}
