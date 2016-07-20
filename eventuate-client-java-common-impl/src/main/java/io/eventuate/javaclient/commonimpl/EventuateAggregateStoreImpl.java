package io.eventuate.javaclient.commonimpl;

import io.eventuate.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EventuateAggregateStoreImpl implements EventuateAggregateStore {

  private AggregateCrud aggregateCrud;
  private AggregateEvents aggregateEvents;
  private SerializedEventDeserializer serializedEventDeserializer = new DefaultSerializedEventDeserializer();

  public EventuateAggregateStoreImpl(AggregateCrud aggregateCrud, AggregateEvents aggregateEvents) {
    this.aggregateCrud = aggregateCrud;
    this.aggregateEvents = aggregateEvents;
  }

  public void setSerializedEventDeserializer(SerializedEventDeserializer serializedEventDeserializer) {
    this.serializedEventDeserializer = serializedEventDeserializer;
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<EntityIdAndVersion> save(Class<T> clasz, List<Event> events) {
    return save(clasz, events, Optional.empty());
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<EntityIdAndVersion> save(Class<T> clasz, List<Event> events, SaveOptions saveOptions) {
    return save(clasz, events, Optional.ofNullable(saveOptions));
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<EntityIdAndVersion> save(Class<T> clasz, List<Event> events, Optional<SaveOptions> saveOptions) {
    return aggregateCrud.save(clasz.getName(), events.stream().map(this::toEventTypeAndData).collect(Collectors.toList()), saveOptions);
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<EntityWithMetadata<T>> find(Class<T> clasz, String entityId) {
    return find(clasz, entityId, Optional.empty());
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<EntityWithMetadata<T>> find(Class<T> clasz, String entityId, FindOptions findOptions) {
    return find(clasz, entityId, Optional.ofNullable(findOptions));
  }

  private EventTypeAndData toEventTypeAndData(Event event) {
    return new EventTypeAndData(event.getClass().getName(), JSonMapper.toJson(event));
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<EntityWithMetadata<T>> find(Class<T> clasz, String entityId, Optional<FindOptions> findOptions) {
    return aggregateCrud.find(clasz.getName(), entityId, findOptions).thenApply(le -> {
      List<Event> events = le.getEvents().stream().map(this::toEvent).collect(Collectors.toList());
      return new EntityWithMetadata<T>(
              new EntityIdAndVersion(entityId, le.getEvents().get(le.getEvents().size() - 1).getId()),
              events,
              Aggregates.recreateAggregate(clasz, events));
    });
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<EntityIdAndVersion> update(Class<T> clasz, EntityIdAndVersion entityIdAndVersion, List<Event> events) {
    return update(clasz, entityIdAndVersion, events, Optional.empty());
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<EntityIdAndVersion> update(Class<T> clasz, EntityIdAndVersion entityIdAndVersion, List<Event> events, UpdateOptions updateOptions) {
    return update(clasz, entityIdAndVersion, events, Optional.ofNullable(updateOptions));
  }

  private Event toEvent(EventIdTypeAndData eventIdTypeAndData) {
    try {
      return JSonMapper.fromJson(eventIdTypeAndData.getEventData(), (Class<Event>) Class.forName(eventIdTypeAndData.getEventType()));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public <T extends Aggregate<T>> CompletableFuture<EntityIdAndVersion> update(Class<T> clasz, EntityIdAndVersion entityIdAndVersion, List<Event> events, Optional<UpdateOptions> updateOptions) {
    return aggregateCrud.update(new EntityIdAndType(entityIdAndVersion.getEntityId(), clasz.getName()),
            entityIdAndVersion.getEntityVersion(),
            events.stream().map(this::toEventTypeAndData).collect(Collectors.toList()),
            updateOptions);
  }

  @Override
  public CompletableFuture<?> subscribe(String subscriberId, Map<String, Set<String>> aggregatesAndEvents, SubscriberOptions subscriberOptions, Function<DispatchedEvent<Event>, CompletableFuture<?>> handler) {
    return aggregateEvents.subscribe(subscriberId, aggregatesAndEvents, subscriberOptions,
            se -> serializedEventDeserializer.toDispatchedEvent(se).map(handler::apply).orElse(CompletableFuture.completedFuture(null)));
  }

}
