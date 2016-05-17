package io.eventuate.javaclient.spring.autoconfiguration;

import io.eventuate.EntityIdAndVersion;
import io.eventuate.Event;
import io.eventuate.EventuateAggregateStore;
import io.eventuate.example.banking.domain.*;
import io.eventuate.example.banking.services.AccountCommandSideEventHandler;
import io.eventuate.example.banking.services.AccountQuerySideEventHandler;
import io.eventuate.example.banking.services.MoneyTransferCommandSideEventHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rx.Observable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AutoConfigurationIntegrationTestConfiguration.class)
@IntegrationTest
public class AutoConfigurationIntegrationTest {


  private Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private EventuateAggregateStore aggregateStore;

  @Autowired
  private AccountCommandSideEventHandler accountCommandSideEventHandler;

  @Autowired
  private AccountQuerySideEventHandler accountQuerySideEventHandler;

  @Autowired
  private MoneyTransferCommandSideEventHandler moneyTransferCommandSideEventHandler;

  @Test
  public void shouldInitialize() throws ExecutionException, InterruptedException {

    Account account = new Account();
    List<Event> accountEvents = account.process(new CreateAccountCommand(new BigDecimal(12345)));


    EntityIdAndVersion accountEntity = aggregateStore.save(Account.class, accountEvents, Optional.empty()).get();

    logger.info("Looking for event: " + accountEntity.getEntityVersion().asString());

    accountCommandSideEventHandler.getEvents().eventuallyContains(ctx -> ctx.getEventId().equals(accountEntity.getEntityVersion()));

    accountQuerySideEventHandler.getEvents().eventuallyContains(ctx -> ctx.getEventId().equals(accountEntity.getEntityVersion()));


    MoneyTransfer moneyTransfer = new MoneyTransfer();
    List<Event> moneyTransferEvents = moneyTransfer.process(new CreateMoneyTransferCommand(new TransferDetails(accountEntity.getEntityId(), accountEntity.getEntityId(), new BigDecimal(1))));

    EntityIdAndVersion moneyTransferEntity = aggregateStore.save(MoneyTransfer.class, moneyTransferEvents, Optional.empty()).get();

    logger.info("Looking for MoneyTransferCreatedEvent: " + moneyTransferEntity.getEntityVersion());

    moneyTransferCommandSideEventHandler.getEvents().eventuallyContains(
            ctx -> ctx.getEventId().equals(moneyTransferEntity.getEntityVersion()));

    logger.info("Looking for AccountDebitedEvent with this transaction id: " + moneyTransferEntity.getEntityId());

    moneyTransferCommandSideEventHandler.getEvents().eventuallyContains(
            ctx -> AccountDebitedEvent.class.isInstance(ctx.getEvent()) && moneyTransferEntity.getEntityId().equals(((AccountDebitedEvent) ctx.getEvent()).getTransactionId()));

    TimeUnit.SECONDS.sleep(10);

  }

  <T> void eventuallyContains(Observable<T> obs, Predicate<T> pred) {
    try {
      obs.timeout(30, TimeUnit.SECONDS)
              .onErrorResumeNext(t -> Observable.error(new RuntimeException("Presumably first timeout failed", t)))
              .filter(pred::test)
              .take(1)
              .timeout(720, TimeUnit.SECONDS).toBlocking().first();
    } catch (Throwable t) {
      logger.error("Failure", t);
      throw new RuntimeException(t);
    }
  }

}
