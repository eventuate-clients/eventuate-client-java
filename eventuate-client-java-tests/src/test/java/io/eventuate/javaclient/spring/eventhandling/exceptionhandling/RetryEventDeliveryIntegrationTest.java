package io.eventuate.javaclient.spring.eventhandling.exceptionhandling;

import io.eventuate.EntityWithIdAndVersion;
import io.eventuate.example.banking.domain.Account;
import io.eventuate.example.banking.domain.AccountCommand;
import io.eventuate.example.banking.domain.CreateAccountCommand;
import io.eventuate.sync.AggregateRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RetryEventDeliveryIntegrationConfiguration.class)
@IntegrationTest
public class RetryEventDeliveryIntegrationTest {


  @Autowired
  private AggregateRepository<Account, AccountCommand> accountRepository;

  @Autowired
  private RetryEventDeliveryIntegrationTestEventHandler retryEventDeliveryIntegrationTestEventHandler;

  @Test
  public void shouldCreateAccount() {
    EntityWithIdAndVersion<Account> saveResult = accountRepository.save(new CreateAccountCommand(new BigDecimal("10.23")));
    retryEventDeliveryIntegrationTestEventHandler.getEvents().eventuallyContains(ctx -> ctx.getEventId().equals(saveResult.getEntityVersion()));
  }
}


