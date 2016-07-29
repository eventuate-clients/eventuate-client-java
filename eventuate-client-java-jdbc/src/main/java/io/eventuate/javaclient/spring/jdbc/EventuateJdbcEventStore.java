package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.*;
import io.eventuate.javaclient.commonimpl.*;
import io.eventuate.DuplicateTriggeringEventException;
import io.eventuate.OptimisticLockingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
public class EventuateJdbcEventStore implements AggregateCrud, AggregateEvents {

  Logger logger = LoggerFactory.getLogger(getClass());

  private JdbcTemplate jdbcTemplate;

  public EventuateJdbcEventStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private AtomicLong idGenerator = new AtomicLong(0);

  private Int128 genId() {
    return new Int128(System.currentTimeMillis(), idGenerator.incrementAndGet());
  }

  @Override
  public CompletableFuture<EntityIdAndVersion> save(String aggregateType, List<EventTypeAndData> events, Optional<SaveOptions> saveOptions) {
    List<EventIdTypeAndData> eventsWithIds = events.stream().map(this::toEventWithId).collect(Collectors.toList());
    String entityId = saveOptions.flatMap(SaveOptions::getEntityId).orElse(genId().asString());

    Int128 entityVersion = last(eventsWithIds).getId();
    jdbcTemplate.update("INSERT INTO entities (entity_type, entity_id, entity_version) VALUES (?, ?, ?)",
            aggregateType, entityId, entityVersion.asString());


    for (EventIdTypeAndData event : eventsWithIds)
      jdbcTemplate.update("INSERT INTO events (event_id, event_type, event_data, entity_type, entity_id, triggering_event) VALUES (?, ?, ?, ?, ?, ?)",
              event.getId().asString(), event.getEventType(), event.getEventData(), aggregateType, entityId,
              saveOptions.flatMap(SaveOptions::getTriggeringEvent).map(EventContext::getEventToken).orElse(null));

    publish(aggregateType, entityId, eventsWithIds);
    return CompletableFuture.completedFuture(new EntityIdAndVersion(entityId, entityVersion));
  }


  private <T> T last(List<T> eventsWithIds) {
    return eventsWithIds.get(eventsWithIds.size() - 1);
  }

  private EventIdTypeAndData toEventWithId(EventTypeAndData eventTypeAndData) {
    return new EventIdTypeAndData(genId(), eventTypeAndData.getEventType(), eventTypeAndData.getEventData());
  }

  class EventAndTrigger {

    public final EventIdTypeAndData event;
    public final String triggeringEvent;

    public EventAndTrigger(EventIdTypeAndData event, String triggeringEvent) {

      this.event = event;
      this.triggeringEvent = triggeringEvent;
    }
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<LoadedEvents> find(String aggregateType, String entityId, Optional<FindOptions> findOptions) {
    List<EventAndTrigger> events = jdbcTemplate.query(
            "SELECT * FROM events where entity_type = ? and entity_id = ? order by event_id asc",
            (rs, rowNum) -> {
              String eventId = rs.getString("event_id");
              String eventType = rs.getString("event_type");
              String eventData = rs.getString("event_data");
              String entityId1 = rs.getString("entity_id");
              String triggeringEvent = rs.getString("triggering_event");
              return new EventAndTrigger(new EventIdTypeAndData(Int128.fromString(eventId), eventType, eventData), triggeringEvent);
            }, aggregateType, entityId
    );
    logger.debug("Loaded {} events", events);
    findOptions.flatMap(FindOptions::getTriggeringEvent).ifPresent(te -> {
      if (events.stream().map(e -> e.triggeringEvent).anyMatch(pe -> te.getEventToken().equals(pe))) {
        throw new DuplicateTriggeringEventException();
      }
    });
    if (events.isEmpty())
      return CompletableFutureUtil.failedFuture(new EntityNotFoundException());
    else
      return CompletableFuture.completedFuture(new LoadedEvents(events.stream().map(e -> e.event).collect(Collectors.toList())));
  }

  @Override
  public CompletableFuture<EntityIdAndVersion> update(EntityIdAndType entityIdAndType, Int128 entityVersion, List<EventTypeAndData> events, Optional<UpdateOptions> updateOptions) {
    List<EventIdTypeAndData> eventsWithIds = events.stream().map(this::toEventWithId).collect(Collectors.toList());

    String entityType = entityIdAndType.getEntityType();
    String entityId = entityIdAndType.getEntityId();

    Int128 updatedEntityVersion = last(eventsWithIds).getId();

    int count = jdbcTemplate.update("UPDATE entities SET entity_version = ? WHERE entity_type = ? and entity_id = ? and entity_version = ?",
            updatedEntityVersion.asString(),
            entityType,
            entityId,
            entityVersion.asString()
    );

    if (count != 1) {
      logger.error("Failed to update entity: {}", count);
      throw new OptimisticLockingException();
    }

    for (EventIdTypeAndData event : eventsWithIds)
      jdbcTemplate.update("INSERT INTO events (event_id, event_type, event_data, entity_type, entity_id, triggering_event) VALUES (?, ?, ?, ?, ?, ?)",
              event.getId().asString(),
              event.getEventType(),
              event.getEventData(),
              entityType,
              entityId,
              updateOptions.flatMap(UpdateOptions::getTriggeringEvent).map(EventContext::getEventToken).orElse(null));
    publish(entityIdAndType.getEntityType(), entityId, eventsWithIds);

    return CompletableFuture.completedFuture(new EntityIdAndVersion(entityId, entityVersion));

  }

  private AtomicLong eventOffset = new AtomicLong();

  private void publish(String aggregateType, String aggregateId, List<EventIdTypeAndData> eventsWithIds) {
    synchronized (aggregateTypeToSubscription) {
      List<Subscription> subscriptions = aggregateTypeToSubscription.get(aggregateType);
      if (subscriptions != null)
        for (Subscription subscription : subscriptions) {
          for (EventIdTypeAndData event : eventsWithIds) {
            if (subscription.isInterestedIn(aggregateType, event.getEventType()))
              subscription.handler.apply(new SerializedEvent(event.getId(), aggregateId, aggregateType, event.getEventData(), event.getEventType(),
                      aggregateId.hashCode() % 8,
                      eventOffset.getAndIncrement(),
                      new EventContext(event.getId().asString())));
          }
        }
    }

  }

  private final Map<String, List<Subscription>> aggregateTypeToSubscription = new HashMap<>();

  class Subscription {

    private final String subscriberId;
    private final Map<String, Set<String>> aggregatesAndEvents;
    private final Function<SerializedEvent, CompletableFuture<?>> handler;

    public Subscription(String subscriberId, Map<String, Set<String>> aggregatesAndEvents, Function<SerializedEvent, CompletableFuture<?>> handler) {

      this.subscriberId = subscriberId;
      this.aggregatesAndEvents = aggregatesAndEvents;
      this.handler = handler;
    }

    public boolean isInterestedIn(String aggregateType, String eventType) {
      return aggregatesAndEvents.get(aggregateType) != null && aggregatesAndEvents.get(aggregateType).contains(eventType);
    }
  }

  @Override
  public CompletableFuture<?> subscribe(String subscriberId, Map<String, Set<String>> aggregatesAndEvents, SubscriberOptions options, Function<SerializedEvent, CompletableFuture<?>> handler) {
    // TODO handle options
    Subscription subscription = new Subscription(subscriberId, aggregatesAndEvents, handler);
    synchronized (aggregateTypeToSubscription) {
      for (String aggregateType : aggregatesAndEvents.keySet()) {
        List<Subscription> existing = aggregateTypeToSubscription.get(aggregateType);
        if (existing == null) {
          existing = new LinkedList<>();
          aggregateTypeToSubscription.put(aggregateType, existing);
        }
        existing.add(subscription);
      }
    }
    return CompletableFuture.completedFuture(null);
  }

}
