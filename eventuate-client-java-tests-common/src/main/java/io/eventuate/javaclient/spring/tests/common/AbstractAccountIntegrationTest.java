package io.eventuate.javaclient.spring.tests.common;

import io.eventuate.Aggregate;
import io.eventuate.EntityIdAndVersion;
import io.eventuate.EntityNotFoundException;
import io.eventuate.EntityWithMetadata;
import io.eventuate.Event;
import io.eventuate.SaveOptions;
import io.eventuate.example.banking.domain.Account;
import io.eventuate.example.banking.domain.AccountDebitedEvent;
import io.eventuate.example.banking.domain.CreateAccountCommand;
import io.eventuate.example.banking.domain.CreateMoneyTransferCommand;
import io.eventuate.example.banking.domain.MoneyTransfer;
import io.eventuate.example.banking.domain.TransferDetails;
import io.eventuate.example.banking.services.AccountCommandSideEventHandler;
import io.eventuate.example.banking.services.AccountQuerySideEventHandler;
import io.eventuate.example.banking.services.MoneyTransferCommandSideEventHandler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public abstract class AbstractAccountIntegrationTest {
  private Logger logger = LoggerFactory.getLogger(getClass());
  @Autowired
  private AccountCommandSideEventHandler accountCommandSideEventHandler;
  @Autowired
  private AccountQuerySideEventHandler accountQuerySideEventHandler;
  @Autowired
  private MoneyTransferCommandSideEventHandler moneyTransferCommandSideEventHandler;

  @Test
  public void shouldStartMoneyTransfer() throws ExecutionException, InterruptedException {

    Account account = new Account();
    List<Event> accountEvents = account.process(new CreateAccountCommand(new BigDecimal(12345)));


    EntityIdAndVersion accountEntity = save(Account.class, accountEvents, Optional.empty());

    logger.debug("Looking for event: " + accountEntity.getEntityVersion().asString());

    accountCommandSideEventHandler.getEvents().eventuallyContains(ctx -> ctx.getEventId().equals(accountEntity.getEntityVersion()));

    accountQuerySideEventHandler.getEvents().eventuallyContains(ctx -> ctx.getEventId().equals(accountEntity.getEntityVersion()));


    MoneyTransfer moneyTransfer = new MoneyTransfer();
    List<Event> moneyTransferEvents = moneyTransfer.process(new CreateMoneyTransferCommand(new TransferDetails(accountEntity.getEntityId(), accountEntity.getEntityId(), new BigDecimal(1))));

    EntityIdAndVersion moneyTransferEntity = save(MoneyTransfer.class, moneyTransferEvents, Optional.empty());

    logger.debug("Looking for MoneyTransferCreatedEvent: " + moneyTransferEntity.getEntityVersion());

    moneyTransferCommandSideEventHandler.getEvents().eventuallyContains(
            ctx -> ctx.getEventId().equals(moneyTransferEntity.getEntityVersion()));

    logger.debug("Looking for AccountDebitedEvent with this transaction id: " + moneyTransferEntity.getEntityId());

    moneyTransferCommandSideEventHandler.getEvents().eventuallyContains(
            ctx -> AccountDebitedEvent.class.isInstance(ctx.getEvent()) && moneyTransferEntity.getEntityId().equals(((AccountDebitedEvent) ctx.getEvent()).getTransactionId()));

    TimeUnit.SECONDS.sleep(10);

  }


  @Test
  public void shouldCreateAccountWithId() throws ExecutionException, InterruptedException {

    Account account = new Account();
    List<Event> accountEvents = account.process(new CreateAccountCommand(new BigDecimal(12345)));


    String accountId = "unique-account-id-" + UUID.randomUUID().toString();

    EntityIdAndVersion accountEntity = save(Account.class, accountEvents, Optional.of(new SaveOptions().withId(Optional.of(accountId))));

    Class<Account> accountClass = Account.class;
    EntityWithMetadata<Account> loadedEntity = find(accountClass, accountId);
    assertEquals(accountEntity.getEntityVersion(), loadedEntity.getEntityIdAndVersion().getEntityVersion());
  }


  @Test(expected = EntityNotFoundException.class)
  public void shouldFailToFindNonExistentAccount() throws Throwable {

    String accountId = "unique-account-id-" + UUID.randomUUID().toString();
    Class<Account> accountClass = Account.class;
    find(accountClass, accountId);
  }

  protected abstract <T extends Aggregate<T>> EntityIdAndVersion save(Class<T> classz, List<Event> events, Optional<SaveOptions> saveOptions);

  protected abstract <T extends Aggregate<T>> EntityWithMetadata<T> find(Class<T> clasz, String entityId);

}
