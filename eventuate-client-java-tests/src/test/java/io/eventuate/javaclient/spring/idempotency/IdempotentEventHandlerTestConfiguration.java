package io.eventuate.javaclient.spring.idempotency;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class IdempotentEventHandlerTestConfiguration {

  @Bean
  public IdempotentEventHandler idempotentEventHandler() {
    return new IdempotentEventHandler();
  }
}
