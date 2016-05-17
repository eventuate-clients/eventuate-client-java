package io.eventuate.example.banking.services;

import io.eventuate.DispatchedEvent;
import io.eventuate.EventHandlerMethod;
import io.eventuate.EventSubscriber;
import io.eventuate.example.banking.domain.AccountCreatedEvent;

@EventSubscriber(id="javaIntegrationTestQuerySideAccountEventHandlers")
public class AccountQuerySideEventHandler {

  private EventTracker<DispatchedEvent<AccountCreatedEvent>> events = EventTracker.create();

  public EventTracker<DispatchedEvent<AccountCreatedEvent>> getEvents() {
    return events;
  }

  @EventHandlerMethod
  public void create(DispatchedEvent<AccountCreatedEvent> de) {
    events.onNext(de);
  }
}
