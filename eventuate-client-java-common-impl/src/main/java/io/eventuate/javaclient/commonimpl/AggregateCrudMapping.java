package io.eventuate.javaclient.commonimpl;

import io.eventuate.Event;
import io.eventuate.FindOptions;
import io.eventuate.Int128;
import io.eventuate.SaveOptions;
import io.eventuate.Snapshot;
import io.eventuate.UpdateOptions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AggregateCrudMapping {

  public static Optional<AggregateCrudFindOptions> toAggregateCrudFindOptions(Optional<FindOptions> findOptions) {
    return findOptions.map(fo -> new AggregateCrudFindOptions(fo.getTriggeringEvent()));
  }

  public static Optional<AggregateCrudSaveOptions> toAggregateCrudSaveOptions(Optional<SaveOptions> saveOptions) {
    return saveOptions.map(so -> new AggregateCrudSaveOptions(so.getEventMetadata().map(JSonMapper::toJson), so.getTriggeringEvent(), so.getEntityId()));
  }

  public static Optional<AggregateCrudUpdateOptions> toAggregateCrudUpdateOptions(Optional<UpdateOptions> updateOptions) {
    return updateOptions.map(uo -> new AggregateCrudUpdateOptions(uo.getTriggeringEvent(),
            uo.getEventMetadata().map(JSonMapper::toJson),
            uo.getSnapshot().map(AggregateCrudMapping::toSerializedSnapshot)));
  }


  public static List<EventIdTypeAndData> toSerializedEventsWithIds(List<EventTypeAndData> serializedEvents, List<Int128> eventIds) {
    return IntStream.range(0, serializedEvents.size()).boxed().map(idx ->
            new EventIdTypeAndData(eventIds.get(idx),
                    serializedEvents.get(idx).getEventType(),
                    serializedEvents.get(idx).getEventData())).collect(Collectors.toList());
  }

  public static SerializedSnapshot toSerializedSnapshot(Snapshot snapshot) {
    return new SerializedSnapshot(snapshot.getClass().getName(), JSonMapper.toJson(snapshot));
  }

  public static Snapshot toSnapshot(SerializedSnapshot serializedSnapshot) {
    Class<?> clasz;
    try {
      clasz = AggregateCrudMapping.class.getClassLoader().loadClass(serializedSnapshot.getSnapshotType());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    return (Snapshot)JSonMapper.fromJson(serializedSnapshot.getJson(), clasz);
  }

  public static EventTypeAndData toEventTypeAndData(Event event) {
    return new EventTypeAndData(event.getClass().getName(), JSonMapper.toJson(event));
  }

  public static Event toEvent(EventIdTypeAndData eventIdTypeAndData) {
    try {
      return JSonMapper.fromJson(eventIdTypeAndData.getEventData(), (Class<Event>) Class.forName(eventIdTypeAndData.getEventType()));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
