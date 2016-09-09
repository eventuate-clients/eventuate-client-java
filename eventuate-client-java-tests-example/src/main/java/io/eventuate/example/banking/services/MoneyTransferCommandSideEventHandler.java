package io.eventuate.example.banking.services;


import io.eventuate.EndOfCurrentEventsReachedEvent;
import io.eventuate.EventHandlerContext;
import io.eventuate.EventHandlerMethod;
import io.eventuate.EventSubscriber;
import io.eventuate.example.banking.domain.AccountDebitedEvent;
import io.eventuate.example.banking.domain.MoneyTransferCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventSubscriber(id="javaIntegrationTestCommandSideMoneyTransferEventHandlers",progressNotifications = true)
public class MoneyTransferCommandSideEventHandler {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private EventTracker<EventHandlerContext<?>> events = EventTracker.create();

  public EventTracker<EventHandlerContext<?>> getEvents() {
    return events;
  }

  @EventHandlerMethod
  public void moneyTransferCreated(EventHandlerContext<MoneyTransferCreatedEvent> ctx) {
    logger.debug("moneyTransferCreated got event {}", ctx.getEventId());
    events.onNext(ctx);
  }

  @EventHandlerMethod
  public void doAnything(EventHandlerContext<AccountDebitedEvent> ctx) {
    logger.debug("doAnything got event {} {}", ctx.getEventId(), ctx.getEvent().getTransactionId());
    events.onNext(ctx);
  }

  @EventHandlerMethod
  public void noteProgress(EventHandlerContext<EndOfCurrentEventsReachedEvent> ctx) {
    logger.debug("noteProgress got event: " + ctx.getEvent());
    events.onNext(ctx);
  }

}

