package io.eventuate.testutil;

import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTestEventHandler {
  protected EventTracker eventTracker = new EventTracker();
  private Logger logger = LoggerFactory.getLogger(getClass());

  public EventTracker getEventTracker() {
    return eventTracker;
  }

  public <T extends Event> DispatchedEvent<T> assertMessagePublished(String entityId, Class<T> eventClass) {
    return eventTracker.assertMessagePublished(entityId, eventClass);
  }
}
