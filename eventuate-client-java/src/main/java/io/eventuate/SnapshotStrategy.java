package io.eventuate;

import java.util.List;
import java.util.Optional;

public interface SnapshotStrategy {

  Class<?> getAggregateClass();

  Optional<Snapshot> possiblySnapshot(Aggregate aggregate, List<Event> oldEvents, List<Event> newEvents);

  Aggregate recreateAggregate(Class<?> clasz, Snapshot snapshot);
}
