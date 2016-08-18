package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.*;
import io.eventuate.javaclient.commonimpl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class AbstractEventuateJdbcAggregateStore implements AggregateCrud {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private JdbcTemplate jdbcTemplate;

  public AbstractEventuateJdbcAggregateStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }


  private IdGenerator idGenerator = new IdGeneratorImpl();

  @Override
  @Transactional
  public CompletableFuture<EntityIdAndVersion> save(String aggregateType, List<EventTypeAndData> events, Optional<SaveOptions> saveOptions) {
    List<EventIdTypeAndData> eventsWithIds = events.stream().map(this::toEventWithId).collect(Collectors.toList());
    String entityId = saveOptions.flatMap(SaveOptions::getEntityId).orElse(idGenerator.genId().asString());

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
    return new EventIdTypeAndData(idGenerator.genId(), eventTypeAndData.getEventType(), eventTypeAndData.getEventData());
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
  @Transactional
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
    Optional<EventAndTrigger> matching = findOptions.
            flatMap(FindOptions::getTriggeringEvent).
            flatMap(te -> events.stream().filter(e -> te.getEventToken().equals(e.triggeringEvent)).findAny());
    if (matching.isPresent()) {
      return CompletableFutureUtil.failedFuture(new DuplicateTriggeringEventException());
    }
    if (events.isEmpty())
      return CompletableFutureUtil.failedFuture(new EntityNotFoundException());
    else
      return CompletableFuture.completedFuture(new LoadedEvents(events.stream().map(e -> e.event).collect(Collectors.toList())));
  }

  @Override
  @Transactional
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
      return CompletableFutureUtil.failedFuture(new OptimisticLockingException(entityIdAndType, entityVersion));
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

  protected abstract void publish(String aggregateType, String aggregateId, List<EventIdTypeAndData> eventsWithIds);


}
