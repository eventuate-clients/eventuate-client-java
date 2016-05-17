package io.eventuate.javaclient.domain;

import io.eventuate.DispatchedEvent;
import io.eventuate.Event;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class EventHandlerProcessorDispatchedEventReturningVoid implements EventHandlerProcessor {

  @Override
  public boolean supports(Method method) {
    return EventHandlerProcessorUtil.isVoidMethodWithOneParameterOfType(method, DispatchedEvent.class);
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
          method.invoke(eventHandler, de);
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
