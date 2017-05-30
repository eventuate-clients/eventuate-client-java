package io.eventuate.javaclient.stompclient;

import io.eventuate.EventuateClientException;
import io.eventuate.SnapshotManager;
import io.eventuate.SnapshotManagerImpl;
import io.eventuate.javaclient.commonimpl.AggregateCrud;
import io.eventuate.javaclient.commonimpl.AggregateEvents;
import io.eventuate.javaclient.commonimpl.EventuateAggregateStoreImpl;
import io.eventuate.javaclient.restclient.EventuateRESTClient;
import io.eventuate.EventuateAggregateStore;
import io.vertx.core.Vertx;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class EventuateAggregateStoreBuilder {

  private EventuateCredentials eventuateCredentials = new EventuateCredentials(
          System.getenv("EVENTUATE_API_KEY_ID"),
          System.getenv("EVENTUATE_API_KEY_SECRET"),
          Optional.ofNullable(System.getenv("EVENTUATE_API_SPACE")).orElseGet(() -> "default")
  );

  public static EventuateAggregateStoreBuilder standard() {
    return new EventuateAggregateStoreBuilder();
  }

  public static EventuateAggregateStore defaultFromEnv() {
    return standard().build();
  }

  public EventuateAggregateStoreBuilder withCredentials(String key, String secret) {
    this.eventuateCredentials.setApiKeyId(key);
    this.eventuateCredentials.setApiKeySecret(secret);
    return this;
  }

  public EventuateAggregateStoreBuilder withSpace(String space) {
    this.eventuateCredentials.setSpace(space);
    return this;
  }

  public EventuateAggregateStore build() {
    if(StringUtils.isEmpty(eventuateCredentials.getApiKeyId())) {
      throw new EventuateClientException("Eventuate apiKey cannot be empty");
    }
    if(StringUtils.isEmpty(eventuateCredentials.getApiKeySecret())) {
      throw new EventuateClientException("Eventuate apiKeySecret cannot be empty");
    }
    if(StringUtils.isEmpty(eventuateCredentials.getSpace())) {
      throw new EventuateClientException("Eventuate space cannot be empty");
    }

    AggregateCrud aggregateCrud = new EventuateRESTClient(vertx(), eventuateCredentials, makeDefaultUrl());
    AggregateEvents aggregateEvents = new EventuateSTOMPClient(vertx(), eventuateCredentials, makeDefaultUrl());
    SnapshotManager snapshotManager = new SnapshotManagerImpl();

    return new EventuateAggregateStoreImpl(aggregateCrud, aggregateEvents, snapshotManager);
  }

  public static Vertx vertx() {
    System.setProperty("vertx.logger-delegate-factory-class-name", io.vertx.core.logging.SLF4JLogDelegateFactory.class.getName());
    return Vertx.vertx();
  }

  public static URI makeDefaultUrl() {
    try {
      return new URI("https://api.eventuate.io");
    } catch (URISyntaxException e) {
      throw new RuntimeException();
    }
  }
}
