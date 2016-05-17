package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.EntityIdAndVersion;
import io.eventuate.Event;
import io.eventuate.EventuateAggregateStore;
import io.eventuate.example.banking.domain.Account;
import io.eventuate.example.banking.services.AccountCommandSideEventHandler;
import io.eventuate.example.banking.services.AccountQuerySideEventHandler;
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
@SpringApplicationConfiguration(classes = JdbcAutoConfigurationIntegrationTestConfiguration.class)
@IntegrationTest
public class JdbcAutoConfigurationIntegrationTest {

  @Autowired
  private EventuateAggregateStore aggregateStore;

  @Autowired
  private AccountCommandSideEventHandler accountCommandSideEventHandler;
  @Autowired
  private AccountQuerySideEventHandler accountQuerySideEventHandler;


  @Test
  public void shouldInitialize() throws ExecutionException, InterruptedException {

    Account account = new Account();
    List<Event> events = account.process(new CreateAccountCommand(new BigDecimal(123)));

    EntityIdAndVersion r = aggregateStore.save(Account.class, events, Optional.empty()).get();


    accountCommandSideEventHandler.getEvents().eventuallyContains(ctx -> ctx.getEventId().equals(r.getEntityVersion()));

    accountQuerySideEventHandler.getEvents().eventuallyContains(ctx -> ctx.getEventId().equals(r.getEntityVersion()));


  }


}
