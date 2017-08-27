package io.eventuate.javaclient.spring.eventhandling.exceptionhandling;

import io.eventuate.example.banking.domain.Account;
import io.eventuate.example.banking.domain.AccountCommand;
import io.eventuate.javaclient.eventhandling.exceptionhandling.EventDeliveryExceptionHandler;
import io.eventuate.javaclient.eventhandling.exceptionhandling.EventuateClientScheduler;
import io.eventuate.javaclient.eventhandling.exceptionhandling.RetryEventDeliveryExceptionHandler;
import io.eventuate.javaclient.spring.autoconfiguration.AutoConfigurationIntegrationTestConfiguration;
import io.eventuate.sync.AggregateRepository;
import io.eventuate.sync.EventuateAggregateStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;

@Configuration
@Import(AutoConfigurationIntegrationTestConfiguration.class)
public class RetryEventDeliveryIntegrationConfiguration {

  @Bean
  public AggregateRepository<Account, AccountCommand> syncAccountRepository(EventuateAggregateStore aggregateStore) {
    return new AggregateRepository<>(Account.class, aggregateStore);
  }

  @Bean
  public RetryEventDeliveryIntegrationTestEventHandler accountMetadataEventHandler() {
    return new RetryEventDeliveryIntegrationTestEventHandler();
  }

  @Bean
  public EventDeliveryExceptionHandler forEventHandlerRetryEventHandler(EventuateClientScheduler scheduler) {
    return new RetryEventDeliveryExceptionHandler(scheduler)
            .withMaxRetries(2)
            .withRetryInterval(Duration.ofSeconds(2))
            .withExceptions(RetryEventDeliveryIntegrationTestException.class);

  }

}
