package io.eventuate.javaclient.domain;

import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    if (eventHandler != null)
      return eventHandler.dispatch(de);
    else {
      RuntimeException ex = new RuntimeException("No handler for event - subscriberId: " + subscriberId + ", " + de.getEventType());
      logger.error("dispatching failure", ex);
      CompletableFuture completableFuture = new CompletableFuture();
      completableFuture.completeExceptionally(ex);
      return completableFuture;
    }
  }
}
