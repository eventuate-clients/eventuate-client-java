package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.Aggregate;
import io.eventuate.DuplicateTriggeringEventException;
import io.eventuate.EntityAlreadyExistsException;
import io.eventuate.EntityIdAndType;
import io.eventuate.EntityNotFoundException;
import io.eventuate.EventContext;
import io.eventuate.Int128;
import io.eventuate.OptimisticLockingException;
import io.eventuate.javaclient.commonimpl.AggregateCrudFindOptions;
import io.eventuate.javaclient.commonimpl.AggregateCrudSaveOptions;
import io.eventuate.javaclient.commonimpl.AggregateCrudUpdateOptions;
import io.eventuate.javaclient.commonimpl.EntityIdVersionAndEventIds;
import io.eventuate.javaclient.commonimpl.EventIdTypeAndData;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;
import io.eventuate.javaclient.commonimpl.SerializedSnapshot;
import io.eventuate.javaclient.commonimpl.SerializedSnapshotWithVersion;
import io.eventuate.javaclient.commonimpl.sync.AggregateCrud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventuateJdbcAccessImpl implements EventuateJdbcAccess {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private JdbcTemplate jdbcTemplate;
  private String entityTable;
  private String eventTable;
  private String snapshotTable;


  public EventuateJdbcAccessImpl(JdbcTemplate jdbcTemplate) {
    this(jdbcTemplate, Optional.empty());
  }

  public EventuateJdbcAccessImpl(JdbcTemplate jdbcTemplate, Optional<String> database) {
    this.jdbcTemplate = jdbcTemplate;

    if (database.isPresent()) {
      entityTable = database.get() + ".entities";
      eventTable = database.get() + ".events";
      snapshotTable = database.get() + ".snapshots";
    } else {
      entityTable = "entities";
      eventTable = "events";
      snapshotTable = "snapshots";
    }
  }

  private IdGenerator idGenerator = new IdGeneratorImpl();

  @Override
  @Transactional
  public SaveUpdateResult save(String aggregateType, List<EventTypeAndData> events, Optional<AggregateCrudSaveOptions> saveOptions) {
    List<EventIdTypeAndData> eventsWithIds = events.stream().map(this::toEventWithId).collect(Collectors.toList());
    String entityId = saveOptions.flatMap(AggregateCrudSaveOptions::getEntityId).orElse(idGenerator.genId().asString());

    Int128 entityVersion = last(eventsWithIds).getId();

    try {
      jdbcTemplate.update(String.format("INSERT INTO %s (entity_type, entity_id, entity_version) VALUES (?, ?, ?)", entityTable),
              aggregateType, entityId, entityVersion.asString());
    } catch (DuplicateKeyException e) {
      throw new EntityAlreadyExistsException();
    }


    for (EventIdTypeAndData event : eventsWithIds)
      jdbcTemplate.update(String.format("INSERT INTO %s (event_id, event_type, event_data, entity_type, entity_id, triggering_event, metadata) VALUES (?, ?, ?, ?, ?, ?, ?)", eventTable),
              event.getId().asString(), event.getEventType(), event.getEventData(), aggregateType, entityId,
              saveOptions.flatMap(AggregateCrudSaveOptions::getTriggeringEvent).map(EventContext::getEventToken).orElse(null),
              event.getMetadata().orElse(null)
              );

    return new SaveUpdateResult(new EntityIdVersionAndEventIds(entityId, entityVersion, eventsWithIds.stream().map(EventIdTypeAndData::getId).collect(Collectors.toList())),
            new PublishableEvents(aggregateType, entityId, eventsWithIds));
  }


  private <T> T last(List<T> eventsWithIds) {
    return eventsWithIds.get(eventsWithIds.size() - 1);
  }

  private EventIdTypeAndData toEventWithId(EventTypeAndData eventTypeAndData) {
    return new EventIdTypeAndData(idGenerator.genId(), eventTypeAndData.getEventType(), eventTypeAndData.getEventData(), eventTypeAndData.getMetadata());
  }

  private final RowMapper<EventAndTrigger> eventAndTriggerRowMapper = (rs, rowNum) -> {
    String eventId = rs.getString("event_id");
    String eventType = rs.getString("event_type");
    String eventData = rs.getString("event_data");
    String entityId1 = rs.getString("entity_id");
    String triggeringEvent = rs.getString("triggering_event");
    Optional<String> metadata = Optional.ofNullable(rs.getString("metadata"));
    return new EventAndTrigger(new EventIdTypeAndData(Int128.fromString(eventId), eventType, eventData, metadata), triggeringEvent);
  };

  @Override
  @Transactional
  public <T extends Aggregate<T>> LoadedEvents find(String aggregateType, String entityId, Optional<AggregateCrudFindOptions> findOptions) {
    Optional<LoadedSnapshot> snapshot = Optional.ofNullable(DataAccessUtils.singleResult(
            jdbcTemplate.query(
                    String.format("select snapshot_type, snapshot_json, entity_version, triggering_Events from %s where entity_type = ? and entity_id = ? order by entity_version desc LIMIT 1", snapshotTable),
                    (rs, rownum) -> {
                      return new LoadedSnapshot(
                              new SerializedSnapshotWithVersion(
                                      new SerializedSnapshot(rs.getString("snapshot_type"), rs.getString("snapshot_json")),
                                      Int128.fromString(rs.getString("entity_version"))),
                              rs.getString("triggering_events"));
                    },
                    aggregateType,
                    entityId
            )
    ));


    snapshot.ifPresent(ss -> {
      findOptions.flatMap(AggregateCrudFindOptions::getTriggeringEvent).ifPresent(te -> {
        checkSnapshotForDuplicateEvent(ss, te);
      });
    });


    List<EventAndTrigger> events;

    if (snapshot.isPresent()) {
      events = jdbcTemplate.query(
              String.format("SELECT * FROM %s where entity_type = ? and entity_id = ? and event_id > ? order by event_id asc", eventTable),
              eventAndTriggerRowMapper, aggregateType, entityId, snapshot.get().getSerializedSnapshot().getEntityVersion().asString()
      );
    } else {
      events = jdbcTemplate.query(
              String.format("SELECT * FROM %s where entity_type = ? and entity_id = ? order by event_id asc", eventTable),
              eventAndTriggerRowMapper, aggregateType, entityId
      );
    }

    logger.debug("Loaded {} events", events);
    Optional<EventAndTrigger> matching = findOptions.
            flatMap(AggregateCrudFindOptions::getTriggeringEvent).
            flatMap(te -> events.stream().filter(e -> te.getEventToken().equals(e.triggeringEvent)).findAny());
    if (matching.isPresent()) {
      throw new DuplicateTriggeringEventException();
    }
    if (!snapshot.isPresent() && events.isEmpty())
      throw new EntityNotFoundException();
    else {
      return new LoadedEvents(snapshot.map(LoadedSnapshot::getSerializedSnapshot), events.stream().map(e -> e.event).collect(Collectors.toList()));
    }
  }


  @Override
  @Transactional
  public SaveUpdateResult update(EntityIdAndType entityIdAndType, Int128 entityVersion, List<EventTypeAndData> events, Optional<AggregateCrudUpdateOptions> updateOptions) {

    // TODO - triggering event check

    List<EventIdTypeAndData> eventsWithIds = events.stream().map(this::toEventWithId).collect(Collectors.toList());

    String entityType = entityIdAndType.getEntityType();
    String aggregateType = entityIdAndType.getEntityType();

    String entityId = entityIdAndType.getEntityId();

    Int128 updatedEntityVersion = last(eventsWithIds).getId();

    int count = jdbcTemplate.update(String.format("UPDATE %s SET entity_version = ? WHERE entity_type = ? and entity_id = ? and entity_version = ?", entityTable),
            updatedEntityVersion.asString(),
            entityType,
            entityId,
            entityVersion.asString()
    );

    if (count != 1) {
      logger.error("Failed to update entity: {}", count);
      throw new OptimisticLockingException(entityIdAndType, entityVersion);
    }

    updateOptions.flatMap(AggregateCrudUpdateOptions::getSnapshot).ifPresent(ss -> {

      Optional<LoadedSnapshot> previousSnapshot = Optional.ofNullable(DataAccessUtils.singleResult(
              jdbcTemplate.query(
                      String.format("select snapshot_type, snapshot_json, entity_version, triggering_Events from %s where entity_type = ? and entity_id = ? order by entity_version desc LIMIT 1", snapshotTable),
                      (rs, rownum) -> {
                        return new LoadedSnapshot(
                                new SerializedSnapshotWithVersion(
                                        new SerializedSnapshot(rs.getString("snapshot_type"), rs.getString("snapshot_json")),
                                        Int128.fromString(rs.getString("entity_version"))), rs.getString("triggering_events"));
                      },
                      aggregateType,
                      entityId
              )
      ));


      List<EventAndTrigger> oldEvents;

      if (previousSnapshot.isPresent()) {
        oldEvents = jdbcTemplate.query(
                String.format("SELECT * FROM %s where entity_type = ? and entity_id = ? and event_id > ? order by event_id asc", eventTable),
                eventAndTriggerRowMapper, aggregateType, entityId, previousSnapshot.get().getSerializedSnapshot().getEntityVersion().asString()
        );
      } else {
        oldEvents = jdbcTemplate.query(
                String.format("SELECT * FROM %s where entity_type = ? and entity_id = ? order by event_id asc", eventTable),
                eventAndTriggerRowMapper, aggregateType, entityId
        );
      }

      String triggeringEvents = snapshotTriggeringEvents(previousSnapshot, oldEvents, updateOptions.flatMap(AggregateCrudUpdateOptions::getTriggeringEvent));

      jdbcTemplate.update(String.format("INSERT INTO %s (entity_type, entity_id, entity_version, snapshot_type, snapshot_json, triggering_events) VALUES (?, ?, ?, ?, ?, ?)", snapshotTable),
              entityType,
              entityId,
              updatedEntityVersion.asString(),
              ss.getSnapshotType(),
              ss.getJson(),
              triggeringEvents);
    });


    for (EventIdTypeAndData event : eventsWithIds)
      jdbcTemplate.update(String.format("INSERT INTO %s (event_id, event_type, event_data, entity_type, entity_id, triggering_event, metadata) VALUES (?, ?, ?, ?, ?, ?, ?)", eventTable),
              event.getId().asString(),
              event.getEventType(),
              event.getEventData(),
              entityType,
              entityId,
              updateOptions.flatMap(AggregateCrudUpdateOptions::getTriggeringEvent).map(EventContext::getEventToken).orElse(null),
              event.getMetadata().orElse(null));


    return new SaveUpdateResult(new EntityIdVersionAndEventIds(entityId,
            updatedEntityVersion,
            eventsWithIds.stream().map(EventIdTypeAndData::getId).collect(Collectors.toList())),
            new PublishableEvents(aggregateType, entityId, eventsWithIds));

  }


  protected void checkSnapshotForDuplicateEvent(LoadedSnapshot ss, EventContext te) {
    // do nothing
  }

  protected String snapshotTriggeringEvents(Optional<LoadedSnapshot> previousSnapshot, List<EventAndTrigger> events, Optional<EventContext> eventContext) {
    // do nothing
    return null;
  }



}
