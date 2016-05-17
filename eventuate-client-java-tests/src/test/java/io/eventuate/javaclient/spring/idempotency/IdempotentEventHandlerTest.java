package io.eventuate.javaclient.spring.idempotency;

import io.eventuate.EntityIdAndVersion;
import io.eventuate.Event;
import io.eventuate.EventuateAggregateStore;
import io.eventuate.example.banking.domain.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IdempotentEventHandlerTestConfiguration.class)
public class IdempotentEventHandlerTest {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private IdempotentEventHandler idempotentEventHandler;

  @Autowired
  private EventuateAggregateStore aggregateStore;


  @Test
  public void shouldTriggerDuplicate() throws InterruptedException, ExecutionException, TimeoutException {
    Account account = new Account();
    List<Event> accountEvents = account.process(new CreateAccountCommand(new BigDecimal(12345)));

    EntityIdAndVersion accountEntity = aggregateStore.save(Account.class, accountEvents, Optional.empty()).get();

    MoneyTransfer moneyTransfer = new MoneyTransfer();
    List<Event> moneyTransferEvents = moneyTransfer.process(new CreateMoneyTransferCommand(new TransferDetails(accountEntity.getEntityId(), accountEntity.getEntityId(), new BigDecimal(1))));

    EntityIdAndVersion moneyTransferEntity = aggregateStore.save(MoneyTransfer.class, moneyTransferEvents, Optional.empty()).get();

    AsyncOutcome outcome = idempotentEventHandler.events.get(15, TimeUnit.SECONDS);
    assertEquals(outcome.before.getEntityIdAndVersion().getEntityVersion(), outcome.after.getEntityIdAndVersion().getEntityVersion());
  }

}
