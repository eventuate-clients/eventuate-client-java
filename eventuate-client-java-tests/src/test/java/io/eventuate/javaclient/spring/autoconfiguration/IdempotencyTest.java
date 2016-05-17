package io.eventuate.javaclient.spring.autoconfiguration;

import io.eventuate.EntityIdAndVersion;
import io.eventuate.EntityWithMetadata;
import io.eventuate.Event;
import io.eventuate.EventuateAggregateStore;
import io.eventuate.example.banking.domain.Account;
import io.eventuate.example.banking.domain.CreateAccountCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AutoConfigurationIntegrationTestConfiguration.class)
@IntegrationTest
public class IdempotencyTest {

  @Autowired
  private EventuateAggregateStore aggregateStore;

  @Test
  public void secondUpdateShouldBeRejected() throws ExecutionException, InterruptedException {

    Account account = new Account();
    List<Event> accountEvents = account.process(new CreateAccountCommand(new BigDecimal(12345)));

    EntityIdAndVersion savedAccountEntity = aggregateStore.save(Account.class, accountEvents, Optional.empty()).get();

    EntityWithMetadata<Account> loadedAccount = aggregateStore.find(Account.class, savedAccountEntity.getEntityId(), Optional.empty()).get();





  }
}
