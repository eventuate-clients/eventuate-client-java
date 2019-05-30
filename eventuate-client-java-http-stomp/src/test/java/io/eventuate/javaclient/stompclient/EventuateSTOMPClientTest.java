package io.eventuate.javaclient.stompclient;


import io.eventuate.common.id.generator.Int128;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EventuateSTOMPClientTest {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private EventuateSTOMPClient client;
  private TrackingStompServer server;
  private EventuateCredentials credentials = new EventuateCredentials("x", "y", "default");
  private static Vertx vertx;
  private static int port;
  private BlockingQueue<Int128> ids;
  private String mySubId = "MySubId";
  private String subId;

  @Before
  public void setUp() {
    ids = new LinkedBlockingDeque<>();
  }

  @BeforeClass
  public static void beforeClass() throws IOException {
    port = PortUtil.findPort();
    vertx = Vertx.vertx();
  }

  @AfterClass
  public static void afterClass() {
    if (vertx != null) vertx.close();
  }

  @After
  public void tearDown() throws ExecutionException, InterruptedException {
    if (server != null)
      server.close();
  }

  private void makeClientAndSubscribeSync() throws URISyntaxException, ExecutionException, InterruptedException {
    makeClient();
    subscribeSync();
  }

  private void makeClient() throws URISyntaxException {
    client = new EventuateSTOMPClient(vertx, credentials, new URI("stomp://localhost:" + port));
  }

  private void subscribeSync() throws InterruptedException, ExecutionException {
    subId = subscribeAsync(mySubId).get();
  }

  private CompletableFuture<String> subscribeAsync(String subscribeId) {
    return (CompletableFuture<String>) client.subscribe(subscribeId,
            Collections.singletonMap("MyEntityType",
                    Collections.singleton("MyEvent")),
            null, se -> {
              ids.add(se.getId());
              return CompletableFuture.completedFuture("x");
            });
  }

  private void makeServer() {
    server = new TrackingStompServer(vertx, port);
    try {
      server.getListenFuture().get(5, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertDisconnected() throws InterruptedException, ExecutionException {
    for (int i = 0; i < 2000; i++) {
      if (!client.isConnected()) break;
      TimeUnit.MILLISECONDS.sleep(50);
    }

    assertFalse(client.isConnected());
  }

  private void assertConnected() throws InterruptedException, ExecutionException {
    for (int i = 0; i < 50; i++) {
      if (client.isConnected()) break;
      TimeUnit.MILLISECONDS.sleep(100);
    }

    assertTrue(client.isConnected());
  }


  @Test
  public void shouldConnect() throws InterruptedException, URISyntaxException, IOException, ExecutionException {

    makeServer();

    makeClientAndSubscribeSync();

    assertConnected();

    server.assertSubscribed();

    List<Int128> receivedIds = new ArrayList<>();

    logger.debug("Sending message");
    for (int i = 0; i < 500; i++) {
      server.sendMessage(subId, "0-" + i);
    }

    while (receivedIds.size() != 500) {
      Int128 x = ids.poll(20, TimeUnit.SECONDS);
      assertNotNull(x);
      receivedIds.add(x);
    }

    List<Int128> sorted = new ArrayList<>(receivedIds);
    sorted.sort((o1, o2) -> o1.asString().compareTo(o2.asString()));

    assertEquals(sorted, receivedIds);
    server.close();


    assertDisconnected();


    makeServer();
    assertConnected();

    server.assertSubscribed();

    client.close();



  }

  @Test
  public void shouldRepeatedlyAttemptToConnect() throws InterruptedException, URISyntaxException, IOException, ExecutionException, TimeoutException {

    makeClient();
    subscribeAsync(mySubId);

    TimeUnit.SECONDS.sleep(5);

    makeServer();

    assertConnected();

    server.assertSubscribed();

    server.close();
    client.close();

  }

  @Test
  public void trySubscribing() throws InterruptedException, URISyntaxException, IOException, ExecutionException, TimeoutException {

    makeServer();
    makeClientAndSubscribeSync();

    assertConnected();

    // server.assertSubscribed();

    client.subscribe("MySubId2",
            Collections.singletonMap("MyEntityType2",
                    Collections.singleton("MyEvent")),
            null, se -> {
              System.out.print("Y");
              return CompletableFuture.completedFuture("x");
            }).get(5, TimeUnit.SECONDS);

    client.close();

    server.close();

  }


  @Test
  public void shouldClose() throws Exception {

    makeServer();

    makeClientAndSubscribeSync();

    assertConnected();

    server.assertSubscribed();

    client.close();

    server.assertClientIsDisconnected();
  }


  @Test
  public void shouldProcessTwoSubscriptions() throws Exception {

    makeServer();

    makeClient();

    CompletableFuture sub1cf = new CompletableFuture();

    String subId1 = ((CompletableFuture<String>) client.subscribe("mySubscribeId1",
            Collections.singletonMap("MyEntityType",
                    Collections.singleton("MyEvent")),
            null, se -> {
              ids.add(se.getId());
              return sub1cf;
            })).get(4, TimeUnit.SECONDS);

    assertConnected();

    CompletableFuture sub2cf = new CompletableFuture();

    String subId2 = ((CompletableFuture<String>) client.subscribe("mySubscribeId2",
            Collections.singletonMap("MyEntityType",
                    Collections.singleton("MyEvent")),
            null, se -> {
              ids.add(se.getId());
              return sub2cf;
            })).get(4, TimeUnit.SECONDS);


    server.assertSubscribed();

    List<Int128> receivedIds = new ArrayList<>();

    server.sendMessage(subId1, "1-2");

    while (receivedIds.size() != 1) {
      Int128 x = ids.poll(20, TimeUnit.SECONDS);
      assertNotNull(x);
      receivedIds.add(x);
    }

    server.sendMessage(subId2, "3-4");

    while (receivedIds.size() != 2) {
      Int128 x = ids.poll(20, TimeUnit.SECONDS);
      assertNotNull(x);
      receivedIds.add(x);
    }

    sub2cf.complete(null);

    server.assertAcked(1);

    sub1cf.complete(null);

    server.assertAcked(2);

    server.close();


    assertDisconnected();


    makeServer();
    assertConnected();

    server.assertSubscribed();

    client.close();



  }

  }
