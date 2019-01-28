package io.eventuate.example.banking.services;


import io.eventuate.EndOfCurrentEventsReachedEvent;
import io.eventuate.EventHandlerContext;
import io.eventuate.EventHandlerMethod;
import io.eventuate.EventSubscriber;
import io.eventuate.example.banking.domain.AccountDebitedEvent;
import io.eventuate.example.banking.domain.MoneyTransferCreatedEvent;
import io.eventuate.example.banking.services.counting.Countable;
import io.eventuate.testutil.AbstractTestEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventSubscriber(id="javaIntegrationTestCommandSideMoneyTransferEventHandlers",progressNotifications = true)
@Countable
public class MoneyTransferCommandSideEventHandler extends AbstractTestEventHandler {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @EventHandlerMethod
  public void moneyTransferCreated(EventHandlerContext<MoneyTransferCreatedEvent> ctx) {
    logger.debug("moneyTransferCreated got event {}", ctx.getEventId());
    add(ctx);
  }

  @EventHandlerMethod
  public void doAnything(EventHandlerContext<AccountDebitedEvent> ctx) {
    logger.debug("doAnything got event {} {}", ctx.getEventId(), ctx.getEvent().getTransactionId());
    add(ctx);
  }

  @EventHandlerMethod
  public void noteProgress(EventHandlerContext<EndOfCurrentEventsReachedEvent> ctx) {
    logger.debug("noteProgress got event: " + ctx.getEvent());
    add(ctx);
  }

}

