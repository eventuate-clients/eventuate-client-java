package io.eventuate.javaclient.spring;

import io.eventuate.EventuateAggregateStore;
import io.eventuate.javaclient.domain.EventHandlerProcessor;
import io.eventuate.javaclient.domain.EventHandlerProcessorDispatchedEventReturningVoid;
import io.eventuate.javaclient.domain.EventHandlerProcessorEventHandlerContextReturningCompletableFuture;
import io.eventuate.javaclient.domain.EventHandlerProcessorEventHandlerContextReturningVoid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * Defines the Spring beans that support event processing
 */
@Configuration
public class EventuateJavaClientDomainConfiguration {

  @Bean
  public EventHandlerBeanPostProcessor eventHandlerBeanPostProcessor(EventDispatcherInitializer eventDispatcherInitializer) {
    return new EventHandlerBeanPostProcessor(eventDispatcherInitializer);
  }

  @Bean
  public EventDispatcherInitializer eventDispatcherInitializer(EventHandlerProcessor[] processors, EventuateAggregateStore aggregateStore) {
    return new EventDispatcherInitializer(processors, aggregateStore, Executors.newCachedThreadPool());
  }

  @Bean
  public EventHandlerProcessor eventHandlerProcessorEventHandlerContextReturningVoid(EventuateAggregateStore aggregateStore) {
    return new EventHandlerProcessorEventHandlerContextReturningVoid(aggregateStore);
  }

  @Bean
  public EventHandlerProcessor eventHandlerProcessorDispatchedEventReturningVoid(EventuateAggregateStore aggregateStore) {
    return new EventHandlerProcessorDispatchedEventReturningVoid();
  }
  @Bean
  public EventHandlerProcessor eventHandlerProcessorEventHandlerContextReturningCompletableFuture(EventuateAggregateStore aggregateStore) {
    return new EventHandlerProcessorEventHandlerContextReturningCompletableFuture(aggregateStore);
  }
}
