package io.eventuate.javaclient.stompclient;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.stomp.Frame;
import io.vertx.ext.stomp.ServerFrame;
import io.vertx.ext.stomp.StompServer;
import io.vertx.ext.stomp.StompServerConnection;
import io.vertx.ext.stomp.StompServerHandler;
import rx.subjects.ReplaySubject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class TrackingStompServer {

  private Logger logger = LoggerFactory.getLogger(getClass());

  public StompServer server;
  private ReplaySubject<ServerFrame> frames = ReplaySubject.create();
  private CompletableFuture<Void> listenFuture = new CompletableFuture<>();
  private volatile boolean clientDisconnected;

  public boolean isClientDisconnected() {
    return clientDisconnected;
  }


  public TrackingStompServer(Vertx vertx, int port) {
    StompServerHandler stompServerHandler = StompServerHandler.create(vertx);

    stompServerHandler.connectHandler(this::handleConnect);
    stompServerHandler.subscribeHandler(this::handleSubscribe);
    stompServerHandler.ackHandler(this::handleAck);
    stompServerHandler.closeHandler(this::handleClose);

    this.server = StompServer.create(vertx)
            .handler(stompServerHandler)
            .listen(port, "0.0.0.0", event -> {
              if (event.cause() == null) {
                listenFuture.complete(null);
              } else {
                listenFuture.completeExceptionally(event.cause());
              }
            });


  }


  public CompletableFuture<Void> getListenFuture() {
    return listenFuture;
  }

  private void handleAck(ServerFrame serverFrame) {
    recordFrame(serverFrame);
    System.out.print("X");
  }

  private int messageIdCounter;

  class ServerSubscription {

    public final StompServerConnection connection;

    public ServerSubscription(StompServerConnection connection) {
      this.connection = connection;
    }
  }

  private ConcurrentHashMap<String, ServerSubscription> subscriptions = new ConcurrentHashMap<>();

  private void handleSubscribe(ServerFrame serverFrame) {
    // if ("MySubId2".equals(serverFrame.frame().getId())) {
    Map<String, String> receiptHeaders = new HashMap<>();
    receiptHeaders.put(Frame.RECEIPT_ID, serverFrame.frame().getReceipt());
    serverFrame.connection().write(new Frame(Frame.Command.RECEIPT, receiptHeaders, null));

    System.out.println("serverFrame.frame().getId()" + serverFrame.frame().getId());

    subscriptions.put(serverFrame.frame().getId(), new ServerSubscription(serverFrame.connection()));
    recordFrame(serverFrame);
    // }
  }


  public void sendMessage(String subscriptionId, String message) {
    Map<String, String> messageHeaders = new HashMap<>();
    messageHeaders.put(Frame.SUBSCRIPTION, subscriptionId);
    messageHeaders.put(Frame.MESSAGE_ID, Integer.toString(messageIdCounter++));
    messageHeaders.put(Frame.ACK, Integer.toString(messageIdCounter++));
    JsonObject body = new JsonObject();
    body.put("id", message);
    String encode = body.encode();
    StompServerConnection connection = subscriptions.get(subscriptionId).connection;
    connection.write(new Frame(Frame.Command.MESSAGE, messageHeaders, Buffer.buffer(encode)));
  }

  private void handleConnect(ServerFrame serverFrame) {
    recordFrame(serverFrame);
    serverFrame.connection().write(new Frame(Frame.Command.CONNECTED, Collections.emptyMap(), null));

  }

  private void recordFrame(ServerFrame serverFrame) {
    frames.onNext(serverFrame);
  }

  public void close() throws ExecutionException, InterruptedException {
    CompletableFuture<Void> f = new CompletableFuture<>();
    server.close(x -> f.complete(null));
    f.get();
  }

  public void assertSubscribed() {
    frames.timeout(30, TimeUnit.SECONDS)
            .filter(frame -> frame.frame().getCommand().equals(Frame.Command.SUBSCRIBE)).take(1).timeout(720, TimeUnit.SECONDS)
            .toBlocking().first();

  }
  public void assertAcked(int count) {
    assertThat(frames.timeout(30, TimeUnit.SECONDS)
            .filter(frame -> frame.frame().getCommand().equals(Frame.Command.ACK))
            .take(5, TimeUnit.SECONDS).toList().toBlocking().first().size(), is(greaterThanOrEqualTo(count)));

  }

  private void handleClose(StompServerConnection stompServerConnection) {
    this.clientDisconnected = true;
  }

  public void assertClientIsDisconnected() {
    rx.Observable.interval(50, TimeUnit.MILLISECONDS)
            .filter(ignore -> isClientDisconnected())
            .take(1).timeout(300, TimeUnit.MILLISECONDS).toBlocking().first();
  }
}
