package io.eventuate.example.banking.services;


import io.eventuate.EventHandlerContext;
import io.eventuate.EventHandlerMethod;
import io.eventuate.EventSubscriber;
import io.eventuate.example.banking.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@EventSubscriber(id="javaIntegrationTestCommandSideAccountEventHandlers")
public class AccountCommandSideEventHandler {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private EventTracker<EventHandlerContext<?>> events = EventTracker.create();

  public EventTracker<EventHandlerContext<?>> getEvents() {
    return events;
  }

  @EventHandlerMethod
  public void doAnything(EventHandlerContext<AccountCreatedEvent> ctx) {
    events.onNext(ctx);
  }

  @EventHandlerMethod
  public CompletableFuture<?> debitAccount(EventHandlerContext<MoneyTransferCreatedEvent> ctx) {
    events.onNext(ctx);
    logger.info("debiting account");
    TransferDetails details = ctx.getEvent().getDetails();
    return ctx.update(Account.class, details.getFromAccountId(),
            new DebitAccountCommand(details.getAmount(), ctx.getEntityId()));
  }
}

