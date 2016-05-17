package io.eventuate.javaclient.spring.idempotency;

import io.eventuate.EntityWithIdAndVersion;
import io.eventuate.EventHandlerContext;
import io.eventuate.EventHandlerMethod;
import io.eventuate.EventSubscriber;
import io.eventuate.example.banking.domain.Account;
import io.eventuate.example.banking.domain.DebitAccountCommand;
import io.eventuate.example.banking.domain.MoneyTransferCreatedEvent;
import io.eventuate.example.banking.domain.TransferDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@EventSubscriber(id="idempotentEventHandler")
public class IdempotentEventHandler {

  private Logger logger = LoggerFactory.getLogger(getClass());

  CompletableFuture<AsyncOutcome> events = new CompletableFuture<>();

  @EventHandlerMethod
  public CompletableFuture<?> performDebit(EventHandlerContext<MoneyTransferCreatedEvent> ctx){
    logger.info("debiting account");
    TransferDetails details = ctx.getEvent().getDetails();
    return ctx.update(Account.class, details.getFromAccountId(),
            new DebitAccountCommand(details.getAmount(), ctx.getEntityId())).thenCompose(ewidv -> {
              CompletableFuture<EntityWithIdAndVersion<Account>> f = ctx.update(Account.class, details.getFromAccountId(),
                      new DebitAccountCommand(details.getAmount(), ctx.getEntityId()));
              f.handle((r, t) -> {
                if (t == null)
                  events.complete(new AsyncOutcome(ewidv, r));
                else
                  events.completeExceptionally(t);
                return null;
              });
              return f;
            }
    );

  }
}
