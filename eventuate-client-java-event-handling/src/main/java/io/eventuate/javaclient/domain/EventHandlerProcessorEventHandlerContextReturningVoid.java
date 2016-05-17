package io.eventuate.javaclient.domain;

import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import io.eventuate.EventHandlerContext;
import io.eventuate.EventuateAggregateStore;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class EventHandlerProcessorEventHandlerContextReturningVoid implements EventHandlerProcessor {

  private EventuateAggregateStore aggregateStore;

  public EventHandlerProcessorEventHandlerContextReturningVoid(EventuateAggregateStore aggregateStore) {
    this.aggregateStore = aggregateStore;
  }

  @Override
  public boolean supports(Method method) {
    return EventHandlerProcessorUtil.isVoidMethodWithOneParameterOfType(method, EventHandlerContext.class);
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
        CompletableFuture<Void> cf = new CompletableFuture<>();
        try {
          method.invoke(eventHandler, new EventHandlerContextImpl(aggregateStore, de));
          cf.complete(null);
        } catch (Throwable e) {
          e.printStackTrace();
          cf.completeExceptionally(e);
        }
        return cf;
      }
    };
  }


}
