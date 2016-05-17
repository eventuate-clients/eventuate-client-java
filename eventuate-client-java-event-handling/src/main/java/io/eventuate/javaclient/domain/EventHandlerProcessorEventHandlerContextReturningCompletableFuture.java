package io.eventuate.javaclient.domain;

import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import io.eventuate.EventHandlerContext;
import io.eventuate.EventuateAggregateStore;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class EventHandlerProcessorEventHandlerContextReturningCompletableFuture implements EventHandlerProcessor {

  private EventuateAggregateStore aggregateStore;

  public EventHandlerProcessorEventHandlerContextReturningCompletableFuture(EventuateAggregateStore aggregateStore) {
    this.aggregateStore = aggregateStore;
  }

  @Override
  public boolean supports(Method method) {
    return EventHandlerProcessorUtil.isMethodWithOneParameterOfTypeReturning(method, EventHandlerContext.class, CompletableFuture.class);
  }

  @Override
  public EventHandler process(Object eventHandler, Method method) {
    return new EventHandler() {
      @Override
      public Class<Event> getEventType() {
        return EventHandlerProcessorUtil.getEventClass(method);
      }

      @Override
      public CompletableFuture<?> dispatch(DispatchedEvent<Event> de) {
        try {
          return (CompletableFuture<?>) method.invoke(eventHandler, new EventHandlerContextImpl(aggregateStore, de));
        } catch (Throwable e) {
          CompletableFuture<Void> cf = new CompletableFuture<>();
          e.printStackTrace();
          cf.completeExceptionally(e);
          return cf;
        }
      }
    };
  }

}
