package io.eventuate.javaclient.domain;

import io.eventuate.CompletableFutureUtil;
import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import io.eventuate.javaclient.commonimpl.EventuateActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.eventuate.javaclient.commonimpl.EventuateActivity.activityLogger;

public class EventDispatcher {

  private static Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

  private final String subscriberId;
  private final Map<Class<?>, EventHandler> eventTypesAndHandlers;

  public EventDispatcher(String subscriberId, Map<Class<?>, EventHandler> eventTypesAndHandlers) {
    this.subscriberId = subscriberId;
    this.eventTypesAndHandlers = eventTypesAndHandlers;
  }

  public CompletableFuture<?> dispatch(DispatchedEvent<Event> de) {
    EventHandler eventHandler = eventTypesAndHandlers.get(de.getEventType());
    if (eventHandler != null) {
      if (activityLogger.isDebugEnabled()) {
        activityLogger.debug("Invoking event handler {} {} {}", subscriberId, de, eventHandler);
        return CompletableFutureUtil.tap(eventHandler.dispatch(de), (result, throwable) -> {
           if (throwable == null)
              activityLogger.debug("Invoked event handler {} {} {}", subscriberId, de, eventHandler);
           else
              activityLogger.debug(String.format("Event handler failed %s %s %s", subscriberId, de, eventHandler), throwable);
        });
      }
      else
        return eventHandler.dispatch(de);
    } else {
      RuntimeException ex = new RuntimeException("No handler for event - subscriberId: " + subscriberId + ", " + de.getEventType());
      logger.error("dispatching failure", ex);
      CompletableFuture completableFuture = new CompletableFuture();
      completableFuture.completeExceptionally(ex);
      return completableFuture;
    }
  }
}
