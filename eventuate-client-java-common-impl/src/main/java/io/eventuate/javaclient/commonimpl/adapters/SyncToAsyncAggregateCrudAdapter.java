package io.eventuate.javaclient.commonimpl.adapters;

import io.eventuate.Aggregate;
import io.eventuate.CompletableFutureUtil;
import io.eventuate.EntityIdAndType;
import io.eventuate.FindOptions;
import io.eventuate.Int128;
import io.eventuate.SaveOptions;
import io.eventuate.UpdateOptions;
import io.eventuate.javaclient.commonimpl.EntityIdVersionAndEventIds;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;
import io.eventuate.javaclient.commonimpl.sync.AggregateCrud;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SyncToAsyncAggregateCrudAdapter implements io.eventuate.javaclient.commonimpl.AggregateCrud {

  private io.eventuate.javaclient.commonimpl.sync.AggregateCrud target;

  public SyncToAsyncAggregateCrudAdapter(AggregateCrud target) {
    this.target = target;
  }

  @Override
  public CompletableFuture<EntityIdVersionAndEventIds> save(String aggregateType, List<EventTypeAndData> events, Optional<SaveOptions> options) {
    try {
      return CompletableFuture.completedFuture(target.save(aggregateType, events, options));
    } catch (Exception e) {
      return CompletableFutureUtil.failedFuture(e);
    }
  }

  @Override
  public <T extends Aggregate<T>> CompletableFuture<LoadedEvents> find(String aggregateType, String entityId, Optional<FindOptions> findOptions) {
    try {
      return CompletableFuture.completedFuture(target.find(aggregateType, entityId, findOptions));
    } catch (Exception e) {
      return CompletableFutureUtil.failedFuture(e);
    }
  }

  @Override
  public CompletableFuture<EntityIdVersionAndEventIds> update(EntityIdAndType entityIdAndType, Int128 entityVersion, List<EventTypeAndData> events, Optional<UpdateOptions> updateOptions) {
    try {
      return CompletableFuture.completedFuture(target.update(entityIdAndType, entityVersion, events, updateOptions));
    } catch (Exception e) {
      return CompletableFutureUtil.failedFuture(e);
    }
  }
}
