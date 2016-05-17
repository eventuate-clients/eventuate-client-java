package io.eventuate.javaclient.spring.httpstomp;

import io.eventuate.javaclient.commonimpl.EventuateAggregateStoreImpl;
import io.eventuate.javaclient.commonimpl.AggregateEvents;
import io.eventuate.EventuateAggregateStore;
import io.eventuate.javaclient.restclient.EventuateRESTClient;
import io.eventuate.javaclient.commonimpl.AggregateCrud;
import io.eventuate.javaclient.stompclient.EventuateSTOMPClient;
import io.vertx.core.Vertx;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the Spring beans to connect to an Eventuate server
 */
@Configuration
@EnableConfigurationProperties(EventuateHttpStompClientConfigurationProperties.class)
public class EventuateHttpStompClientConfiguration {

  @Bean
  public EventuateAggregateStore httpStompEventStore(AggregateCrud restClient, AggregateEvents stompClient) {
    return new EventuateAggregateStoreImpl(restClient, stompClient);
  }

  @Bean
  public Vertx vertx() {
    System.setProperty("vertx.logger-delegate-factory-class-name", io.vertx.core.logging.SLF4JLogDelegateFactory.class.getName());
    return Vertx.vertx();
  }

  @Bean
  public AggregateCrud restClient(Vertx vertx, EventuateHttpStompClientConfigurationProperties config) {
    return new EventuateRESTClient(vertx, config.makeCredentials(), config.getUrl());
  }

  @Bean
  public AggregateEvents stompClient(Vertx vertx, EventuateHttpStompClientConfigurationProperties config) {
    return new EventuateSTOMPClient(vertx, config.makeCredentials(), config.makeStompUrl());
  }
}
