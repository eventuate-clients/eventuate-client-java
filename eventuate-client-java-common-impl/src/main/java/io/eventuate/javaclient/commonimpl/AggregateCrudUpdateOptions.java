package io.eventuate.javaclient.commonimpl;

import io.eventuate.EventContext;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Optional;

public class AggregateCrudUpdateOptions {

  private final Optional<EventContext> triggeringEvent;
  private final Optional<String> eventMetadata;
  private final Optional<SerializedSnapshot> snapshot;

  public AggregateCrudUpdateOptions() {
    this.triggeringEvent = Optional.empty();
    this.eventMetadata = Optional.empty();
    this.snapshot = Optional.empty();
  }

  public AggregateCrudUpdateOptions(Optional<EventContext> triggeringEvent, Optional<String> eventMetadata, Optional<SerializedSnapshot> snapshot) {
    this.triggeringEvent = triggeringEvent;
    this.eventMetadata = eventMetadata;
    this.snapshot = snapshot;
  }

  public AggregateCrudUpdateOptions withSnapshot(SerializedSnapshot serializedSnapshot) {
    return new AggregateCrudUpdateOptions(this.triggeringEvent, this.eventMetadata, Optional.of(serializedSnapshot));
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public Optional<EventContext> getTriggeringEvent() {
    return triggeringEvent;
  }

  public Optional<String> getEventMetadata() {
    return eventMetadata;
  }

  public Optional<SerializedSnapshot> getSnapshot() {
    return snapshot;
  }

  public AggregateCrudUpdateOptions withTriggeringEvent(EventContext eventContext) {
    return new AggregateCrudUpdateOptions(Optional.of(eventContext), this.eventMetadata, this.snapshot);
  }
}
