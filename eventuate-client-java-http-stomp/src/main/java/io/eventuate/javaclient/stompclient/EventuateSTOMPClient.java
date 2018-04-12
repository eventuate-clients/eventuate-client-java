package io.eventuate.javaclient.stompclient;

import io.eventuate.SubscriberOptions;
import io.eventuate.javaclient.commonimpl.AggregateEvents;
import io.eventuate.javaclient.commonimpl.JSonMapper;
import io.eventuate.javaclient.commonimpl.SerializedEvent;
import io.eventuate.EventContext;
import io.eventuate.javaclient.restclient.VertxUtil;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.TCPSSLOptions;
import io.vertx.ext.stomp.Frame;
import io.vertx.ext.stomp.StompClientConnection;
import io.vertx.ext.stomp.StompClientOptions;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class EventuateSTOMPClient implements AggregateEvents {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private Vertx vertx;
  private final Context context;
  private EventuateCredentials eventuateCredentials;

  private MyStompClientImpl stompClient;
  private String host;
  private boolean useSsl;
  private int port;
  private Map<String, String> customConnectHeaders;

  public EventuateSTOMPClient(Vertx vertx, EventuateCredentials eventuateCredentials, URI uri) {
    this(vertx, eventuateCredentials, uri, Collections.emptyMap());
  }

  public EventuateSTOMPClient(Vertx vertx, EventuateCredentials eventuateCredentials, URI uri, Map<String, String> customConnectHeaders) {
    this.vertx = vertx;
    this.context = VertxUtil.getContext(vertx);
    this.eventuateCredentials = eventuateCredentials;
    this.host = uri.getHost();
    this.port = uri.getPort();
    this.useSsl = uri.getScheme().startsWith("stomp+ssl");
    this.customConnectHeaders = customConnectHeaders;
    if (logger.isInfoEnabled())
      logger.debug("STOMP connection: " + Arrays.asList(host, port, useSsl));
  }

  public void initialize() {
    state.status = ConnectionStatus.CONNECTING;

    StompClientOptions options = new StompClientOptions();
    options.setHost(host);  // TODO - fixme
    options.setSsl(useSsl);

    // TODO OMG - stupid bug in 3.2.1

    try {
      Field f = TCPSSLOptions.class.getDeclaredField("ssl");
      f.setAccessible(true);
      f.set(options, useSsl);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    options.setPort(port); // TODO - fixme
    options.setLogin(eventuateCredentials.getApiKeyId());
    options.setPasscode(eventuateCredentials.getApiKeySecret());
    options.setHeartbeat(new JsonObject().put("x", 60 * 1000).put("y", 60 * 1000));

    stompClient = new MyStompClientImpl(vertx, options, customConnectHeaders);

    stompClient.closeHandler(this::handleClose);

    logger.debug("Connecting...");

    stompClient.connect(x -> {
      if (x.succeeded()) {
        logger.debug("Connected!");
        handleConnectSucceeded(x.result());
      } else {
        logger.error("Connect attempt failed", x.cause());
        handleConnectFailed();
      }
    });
  }

  private void handleConnectFailed() {
    if (state.status != ConnectionStatus.CLOSED) {
      state.status = ConnectionStatus.CONNECTION_FAILED_WAITING_TO_RETRY;
      vertx.setTimer(1000, timerX -> {
        if (state.status == ConnectionStatus.CONNECTION_FAILED_WAITING_TO_RETRY) {
          state.status = ConnectionStatus.CONNECTING;
          logger.debug("Attempting to connecting...");
          initialize();
        }
      });
    }
  }


  public void handleClose(Void x) {
    if (state.status != ConnectionStatus.CLOSED) {
      logger.debug("Reconnecting...");
      state.status = ConnectionStatus.CONNECTING;
      initialize();
    }
  }

  private void handleConnectSucceeded(StompClientConnection connection) {
    if (state.status != ConnectionStatus.CLOSED) {
      connection.errorHandler(frame -> {
        logger.error("got error frame: " + frame.toString());
      });

      state.status = ConnectionStatus.CONNECTED;
      state.connection = connection;
      for (int i = 0; i < state.pendingSubscriptions.size(); i++) {
        doSubscribe(state.pendingSubscriptions.get(i));
      }
    }
  }


  private String makeDestinationHeader(Subscription sub) {
    SubscriptionIdWithSpace x =
            new SubscriptionIdWithSpace(sub.subscriberId, sub.space, sub.aggregatesAndEvents, sub.getSubscriberOptions());
    return JSonMapper.toJson(x);
  }


  private void frameHandler(Frame frame, Subscription sub) {
    String bodyAsString = frame.getBodyAsString();
    if (logger.isTraceEnabled())
      logger.trace("Got body=" + bodyAsString);
    StompEvent stompEvent = JSonMapper.fromJson(bodyAsString, StompEvent.class);
    SerializedEvent serializedEvent = makeSerializedEvent(stompEvent);

    handleEvent(sub, serializedEvent, frame.getAck());
  }

  private void handleEvent(Subscription sub, SerializedEvent serializedEvent, String ackHeader) {
    sub.ackOrderTracker.add(ackHeader);

    sub.handler.apply(serializedEvent).handle((result, e) -> {
      if (e != null) {
        // TODO - what else to do here???
        logger.error("Failed handler for subscription {} for event {}", e, sub.subscriberId, serializedEvent);
      } else {
        logger.trace("Successfully completed handler for subscription {} for event {}", sub.subscriberId, serializedEvent);
        context.runOnContext(ignored -> {
          for (String ah : sub.ackOrderTracker.ack(ackHeader)) {
            if (logger.isTraceEnabled())
              logger.trace("Sending acknowledgement: " + ah);
            state.connection.ack(ah);
          }
          logger.trace("Pending ack headers {} {}", sub.subscriberId, sub.ackOrderTracker.getPendingHeaders().size());
        });
      }
      return null;
    });

  }

  private SerializedEvent makeSerializedEvent(StompEvent stompEvent) {
    return new SerializedEvent(
            stompEvent.getId(), stompEvent.getEntityId(), stompEvent.getEntityType(),
            stompEvent.getEventData(), stompEvent.getEventType(),
            stompEvent.getSwimlane(),
            stompEvent.getOffset(),
            new EventContext(stompEvent.getEventToken()),
            Optional.empty());
  }

  public boolean isConnected() throws ExecutionException, InterruptedException {
    CompletableFuture<Boolean> outcome = new CompletableFuture<>();
    context.runOnContext(ignored -> {
       outcome.complete(state.status == ConnectionStatus.CONNECTED);
    });
    return outcome.get();
  }

  public CompletableFuture<Void> close() {
    CompletableFuture<Void> outcome = new CompletableFuture<>();
    context.runOnContext(x -> {
      switch (state.status) {

        case UNCONNECTED:
          // TODO
          break;

        case CONNECTING:
        case CONNECTED:
          stompClient.close();
          state.status = ConnectionStatus.CLOSED;
          outcome.complete(null);
          break;

        default:
          outcome.completeExceptionally(new UnsupportedOperationException("Do not know what to do with this state: " + state.status));
      }
    });
    return outcome;
  }

  enum ConnectionStatus {UNCONNECTED, CONNECTED, CONNECTION_FAILED_WAITING_TO_RETRY, CLOSED, CONNECTING}

  public AckOrderTracker oneAckOrderTracker = new AckOrderTracker();

  class Subscription {
    private String uniqueId = UUID.randomUUID().toString();
    private String subscriberId;
    private Map<String, Set<String>> aggregatesAndEvents;
    private Function<SerializedEvent, CompletableFuture<?>> handler;
    private CompletableFuture<Object> subscribeCompletedFuture = new CompletableFuture<>();
    private SubscriberOptions subscriberOptions;
    public String space;
    public AckOrderTracker ackOrderTracker = oneAckOrderTracker;


    public Subscription(String subscriberId, String space, Map<String, Set<String>> aggregatesAndEvents, SubscriberOptions subscriberOptions, Function<SerializedEvent, CompletableFuture<?>> handler) {
      this.subscriberId = subscriberId;
      this.space = space;
      this.aggregatesAndEvents = aggregatesAndEvents;
      this.subscriberOptions = subscriberOptions;
      this.handler = handler;
    }

    public CompletableFuture<?> getSubscribeCompletedFuture() {
      return subscribeCompletedFuture;
    }


    public SubscriberOptions getSubscriberOptions() {
      return subscriberOptions;
    }

    public void noteSubscribed() {
      ackOrderTracker = new AckOrderTracker();
      if (!subscribeCompletedFuture.isDone())
        subscribeCompletedFuture.complete(uniqueId);
    }
  }

  class ConnectionState {
    private ConnectionStatus status = ConnectionStatus.UNCONNECTED;
    private List<Subscription> pendingSubscriptions = new LinkedList<>();
    public StompClientConnection connection;

    public void queue(Subscription sub) {
      pendingSubscriptions.add(sub);
    }
  }

  private final ConnectionState state = new ConnectionState();

  @Override
  public CompletableFuture<?> subscribe(String subscriberId, Map<String, Set<String>> aggregatesAndEvents, SubscriberOptions subscriberOptions, Function<SerializedEvent, CompletableFuture<?>> handler) {
    Subscription sub = new Subscription(subscriberId, eventuateCredentials.getSpace(), aggregatesAndEvents, subscriberOptions, handler);

    context.runOnContext(x -> {
      switch (state.status) {

        case UNCONNECTED:
          state.queue(sub);
          initialize();
          break;

        case CONNECTING:
          state.queue(sub);
          break;

        case CONNECTED:
          state.queue(sub);
          doSubscribe(sub);
          break;

        default:
          sub.getSubscribeCompletedFuture().completeExceptionally(new UnsupportedOperationException("Do not know what to do with this state: " + state.status));
      }
    });
    return sub.getSubscribeCompletedFuture();
  }

  private void doSubscribe(Subscription sub) {
    if (logger.isInfoEnabled())
      logger.debug("subscribing  ... " + sub.subscriberId);

    Map<String, String> headers = new HashMap<>();
    headers.put(Frame.ID, sub.uniqueId);

    String destination = makeDestinationHeader(sub);
    state.connection.subscribe(destination, headers,
            frame -> frameHandler(frame, sub), rh -> {
              if (logger.isInfoEnabled())
                logger.debug("Subscribed: " + sub.subscriberId);
              sub.noteSubscribed();
            });

  }

}
