package io.eventuate.javaclient.commonimpl.adapters;

import io.eventuate.CompletableFutureUtil;
import io.eventuate.SubscriberOptions;
import io.eventuate.javaclient.commonimpl.SerializedEvent;
import io.eventuate.javaclient.commonimpl.sync.AggregateEvents;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SyncToAsyncAggregateEventsAdapter implements io.eventuate.javaclient.commonimpl.AggregateEvents {

  private io.eventuate.javaclient.commonimpl.sync.AggregateEvents target;

  public SyncToAsyncAggregateEventsAdapter(AggregateEvents target) {
    this.target = target;
  }

  @Override
  public CompletableFuture<?> subscribe(String subscriberId, Map<String, Set<String>> aggregatesAndEvents, SubscriberOptions subscriberOptions, Function<SerializedEvent, CompletableFuture<?>> handler) {
    try {
      target.subscribe(subscriberId, aggregatesAndEvents, subscriberOptions, handler);
      return CompletableFuture.completedFuture(null);
    } catch (RuntimeException e) {
      return CompletableFutureUtil.failedFuture(e);
    }
  }
}
