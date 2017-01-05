package io.eventuate.javaclient.commonimpl;

import io.eventuate.EventContext;

import java.util.Optional;

public class AggregateCrudSaveOptions {

  private final Optional<String> entityId;
  private final Optional<EventContext> triggeringEvent;
  private final Optional<String> eventMetadata;

  public AggregateCrudSaveOptions() {
    this.entityId = Optional.empty();
    this.triggeringEvent = Optional.empty();
    this.eventMetadata = Optional.empty();
  }

  public AggregateCrudSaveOptions(Optional<String> eventMetadata, Optional<EventContext> triggeringEvent, Optional<String> entityId) {
    this.eventMetadata = eventMetadata;
    this.triggeringEvent = triggeringEvent;
    this.entityId = entityId;
  }

  public Optional<String> getEntityId() {
    return entityId;
  }

  public Optional<EventContext> getTriggeringEvent() {
    return triggeringEvent;
  }

  public Optional<String> getEventMetadata() {
    return eventMetadata;
  }

  public AggregateCrudSaveOptions withEventContext(EventContext ectx) {
    return new AggregateCrudSaveOptions(this.eventMetadata, Optional.of(ectx), this.entityId);

  }

  public AggregateCrudSaveOptions withId(String entityId) {
    return new AggregateCrudSaveOptions(this.eventMetadata, this.triggeringEvent, Optional.of(entityId));
  }
}
