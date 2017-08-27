package io.eventuate.javaclient.driver;

import io.eventuate.javaclient.commonimpl.AggregateCrud;
import io.eventuate.javaclient.commonimpl.AggregateEvents;
import io.eventuate.javaclient.commonimpl.adapters.AsyncToSyncAggregateCrudAdapter;
import io.eventuate.javaclient.commonimpl.adapters.AsyncToSyncAggregateEventsAdapter;
import io.eventuate.javaclient.commonimpl.adapters.AsyncToSyncTimeoutOptions;
import io.eventuate.javaclient.eventhandling.exceptionhandling.EventuateClientScheduler;
import io.eventuate.javaclient.restclient.EventuateRESTClient;
import io.eventuate.javaclient.spring.common.EventuateCommonConfiguration;
import io.eventuate.javaclient.spring.httpstomp.EventuateHttpStompClientConfigurationProperties;
import io.eventuate.javaclient.stompclient.EventuateSTOMPClient;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(EventuateHttpStompClientConfigurationProperties.class)
@Import(EventuateCommonConfiguration.class)
public class EventuateDriverConfiguration {

  @Autowired(required=false)
  private AsyncToSyncTimeoutOptions timeoutOptions;

  @Bean
  public Vertx vertx() {
    System.setProperty("vertx.logger-delegate-factory-class-name", io.vertx.core.logging.SLF4JLogDelegateFactory.class.getName());
    return Vertx.vertx();
  }

  @Bean
  public AggregateCrud aggregateCrud(Vertx vertx, EventuateHttpStompClientConfigurationProperties config) {
    return new EventuateRESTClient(vertx, config.makeCredentials(), config.getUrl());
  }

  @Bean
  public AggregateEvents aggregateEvents(Vertx vertx, EventuateHttpStompClientConfigurationProperties config) {
    return new EventuateSTOMPClient(vertx, config.makeCredentials(), config.makeStompUrl());
  }

  @Bean
  public io.eventuate.javaclient.commonimpl.sync.AggregateCrud syncAggregateCrud(AggregateCrud restClient) {
    AsyncToSyncAggregateCrudAdapter adapter = new AsyncToSyncAggregateCrudAdapter(restClient);
    if (timeoutOptions != null)
      adapter.setTimeoutOptions(timeoutOptions);
    return adapter;
  }

  @Bean
  public io.eventuate.javaclient.commonimpl.sync.AggregateEvents syncAggregateEvents(AggregateEvents stompClient) {
    AsyncToSyncAggregateEventsAdapter adapter = new AsyncToSyncAggregateEventsAdapter(stompClient);
    if (timeoutOptions != null)
      adapter.setTimeoutOptions(timeoutOptions);
    return adapter;
  }

  @Bean
  public EventuateClientScheduler eventHandlerRecoveryScheduler(Vertx vertx) {
    return new VertxEventuateClientScheduler(vertx);
  }

}
