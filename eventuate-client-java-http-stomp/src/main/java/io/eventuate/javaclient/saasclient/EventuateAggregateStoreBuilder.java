package io.eventuate.javaclient.saasclient;

import io.eventuate.EventuateAggregateStore;
import io.eventuate.EventuateClientException;
import io.eventuate.SnapshotManager;
import io.eventuate.SnapshotManagerImpl;
import io.eventuate.javaclient.commonimpl.AggregateCrud;
import io.eventuate.javaclient.commonimpl.AggregateEvents;
import io.eventuate.javaclient.commonimpl.EventuateAggregateStoreImpl;
import io.eventuate.javaclient.commonimpl.adapters.AsyncToSyncAggregateCrudAdapter;
import io.eventuate.javaclient.commonimpl.adapters.AsyncToSyncAggregateEventsAdapter;
import io.eventuate.javaclient.restclient.EventuateRESTClient;
import io.eventuate.javaclient.stompclient.EventuateCredentials;
import io.eventuate.javaclient.stompclient.EventuateSTOMPClient;
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

  private Vertx vertx;

  public static EventuateAggregateStoreBuilder standard() {
    return new EventuateAggregateStoreBuilder();
  }

  public static EventuateAggregateStore defaultFromEnv() {
    return standard().build();
  }

  public EventuateAggregateStoreBuilder withCredentials(String key, String secret) {
    validateKeyAndSecret(key, secret);

    this.eventuateCredentials.setApiKeyId(key);
    this.eventuateCredentials.setApiKeySecret(secret);
    return this;
  }

  public EventuateAggregateStoreBuilder withSpace(String space) {
    validateSpace(space);

    this.eventuateCredentials.setSpace(space);
    return this;
  }

  public EventuateAggregateStoreBuilder withVertx(Vertx vertx) {
    if (vertx == null) {
      throw new EventuateClientException("Vertx instance cannot be null");
    }

    this.vertx = vertx;
    return this;
  }

  public EventuateAggregateStore build() {
    validateKeyAndSecret(eventuateCredentials.getApiKeyId(), eventuateCredentials.getApiKeySecret());
    validateSpace(eventuateCredentials.getSpace());

    if (this.vertx == null) {
      this.vertx = vertx();
    }

    AggregateCrud aggregateCrud = new EventuateRESTClient(vertx, eventuateCredentials, makeDefaultUrl());
    AggregateEvents aggregateEvents = new EventuateSTOMPClient(vertx, eventuateCredentials, makeDefaultUrl());
    SnapshotManager snapshotManager = new SnapshotManagerImpl();

    return new EventuateAggregateStoreImpl(aggregateCrud, aggregateEvents, snapshotManager);
  }

  public io.eventuate.sync.EventuateAggregateStore buildSync() {
    validateKeyAndSecret(eventuateCredentials.getApiKeyId(), eventuateCredentials.getApiKeySecret());
    validateSpace(eventuateCredentials.getSpace());

    if (this.vertx == null) {
      this.vertx = vertx();
    }

    io.eventuate.javaclient.commonimpl.sync.AggregateCrud aggregateCrud =
            new AsyncToSyncAggregateCrudAdapter(new EventuateRESTClient(vertx, eventuateCredentials, makeDefaultUrl()));
    io.eventuate.javaclient.commonimpl.sync.AggregateEvents aggregateEvents =
            new AsyncToSyncAggregateEventsAdapter(new EventuateSTOMPClient(vertx, eventuateCredentials, makeDefaultUrl()));
    SnapshotManager snapshotManager = new SnapshotManagerImpl();

    return new io.eventuate.javaclient.commonimpl.sync.EventuateAggregateStoreImpl(aggregateCrud, aggregateEvents, snapshotManager);
  }

  private static Vertx vertx() {
    System.setProperty("vertx.logger-delegate-factory-class-name",
            io.vertx.core.logging.SLF4JLogDelegateFactory.class.getName());
    return Vertx.vertx();
  }

  public static URI makeDefaultUrl() {
    try {
      return new URI("https://api.eventuate.io");
    } catch (URISyntaxException e) {
      throw new RuntimeException();
    }
  }

  private void validateKeyAndSecret(String key, String secret) {
    if (StringUtils.isBlank(key)) {
      throw new EventuateClientException("Eventuate apiKey cannot be empty");
    }
    if (StringUtils.isBlank(secret)) {
      throw new EventuateClientException("Eventuate apiKeySecret cannot be empty");
    }
  }

  private void validateSpace(String space) {
    if (StringUtils.isBlank(space)) {
      throw new EventuateClientException("Eventuate space cannot be empty");
    }
  }
}
