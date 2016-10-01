package io.eventuate.javaclient.commonimpl.sync;

import io.eventuate.Aggregate;
import io.eventuate.EntityIdAndType;
import io.eventuate.FindOptions;
import io.eventuate.Int128;
import io.eventuate.SaveOptions;
import io.eventuate.UpdateOptions;
import io.eventuate.javaclient.commonimpl.EntityIdVersionAndEventIds;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AggregateCrud {
  EntityIdVersionAndEventIds save(String aggregateType, List<EventTypeAndData> events, Optional<SaveOptions> options);

  <T extends Aggregate<T>> LoadedEvents find(String aggregateType, String entityId, Optional<FindOptions> findOptions);

  EntityIdVersionAndEventIds update(EntityIdAndType entityIdAndType, Int128 entityVersion, List<EventTypeAndData> events, Optional<UpdateOptions> updateOptions);
}
