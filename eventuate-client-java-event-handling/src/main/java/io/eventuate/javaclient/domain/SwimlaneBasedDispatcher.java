package io.eventuate.javaclient.domain;

import io.eventuate.DispatchedEvent;
import io.eventuate.Event;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class SwimlaneBasedDispatcher {

  private final ConcurrentHashMap<Integer, SwimlaneDispatcher> map = new ConcurrentHashMap<>();
  private Executor executor;
  private String subscriberId;

  public SwimlaneBasedDispatcher(String subscriberId, Executor executor) {
    this.subscriberId = subscriberId;
    this.executor = executor;
  }

  public CompletableFuture<?> dispatch(DispatchedEvent<Event> de, Function<DispatchedEvent<Event>, CompletableFuture<?>> target) {
    Integer swimlane = de.getSwimlane();
    SwimlaneDispatcher stuff = map.get(swimlane);
    if (stuff == null) {
      stuff = new SwimlaneDispatcher(subscriberId, swimlane, executor);
      SwimlaneDispatcher r = map.putIfAbsent(swimlane, stuff);
      if (r != null)
        stuff = r;
    }
    return stuff.dispatch(de, target);
  }
}

