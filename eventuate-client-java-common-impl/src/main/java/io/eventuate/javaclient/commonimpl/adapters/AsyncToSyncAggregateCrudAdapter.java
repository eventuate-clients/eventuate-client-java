package io.eventuate.javaclient.commonimpl.adapters;

import io.eventuate.Aggregate;
import io.eventuate.CompletableFutureUtil;
import io.eventuate.EntityIdAndType;
import io.eventuate.FindOptions;
import io.eventuate.Int128;
import io.eventuate.SaveOptions;
import io.eventuate.UpdateOptions;
import io.eventuate.javaclient.commonimpl.AggregateCrud;
import io.eventuate.javaclient.commonimpl.EntityIdVersionAndEventIds;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;

import java.util.List;
import java.util.Optional;

public class AsyncToSyncAggregateCrudAdapter implements io.eventuate.javaclient.commonimpl.sync.AggregateCrud {

  private io.eventuate.javaclient.commonimpl.AggregateCrud target;

  private AsyncToSyncTimeoutOptions timeoutOptions = new AsyncToSyncTimeoutOptions();

  public AsyncToSyncAggregateCrudAdapter(AggregateCrud target) {
    this.target = target;
  }

  @Override
  public EntityIdVersionAndEventIds save(String aggregateType, List<EventTypeAndData> events, Optional<SaveOptions> options) {
    try {
      return target.save(aggregateType, events, options).get(timeoutOptions.getTimeout(), timeoutOptions.getTimeUnit());
    } catch (Throwable e) {
      Throwable unwrapped = CompletableFutureUtil.unwrap(e);
      if (unwrapped instanceof RuntimeException)
        throw (RuntimeException)unwrapped;
      else
        throw new RuntimeException(unwrapped);
    }
  }

  @Override
  public <T extends Aggregate<T>> LoadedEvents find(String aggregateType, String entityId, Optional<FindOptions> findOptions) {
    try {
      return target.find(aggregateType, entityId, findOptions).get(timeoutOptions.getTimeout(), timeoutOptions.getTimeUnit());
    } catch (Throwable e) {
      Throwable unwrapped = CompletableFutureUtil.unwrap(e);
      if (unwrapped instanceof RuntimeException)
        throw (RuntimeException)unwrapped;
      else
        throw new RuntimeException(unwrapped);
    }
  }

  @Override
  public EntityIdVersionAndEventIds update(EntityIdAndType entityIdAndType, Int128 entityVersion, List<EventTypeAndData> events, Optional<UpdateOptions> updateOptions) {
    try {
      return target.update(entityIdAndType, entityVersion, events, updateOptions).get(timeoutOptions.getTimeout(), timeoutOptions.getTimeUnit());
    } catch (Throwable e) {
      Throwable unwrapped = CompletableFutureUtil.unwrap(e);
      if (unwrapped instanceof RuntimeException)
        throw (RuntimeException)unwrapped;
      else
        throw new RuntimeException(unwrapped);
    }
  }

  public void setTimeoutOptions(AsyncToSyncTimeoutOptions timeoutOptions) {
    this.timeoutOptions = timeoutOptions;
  }
}
