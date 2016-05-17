package io.eventuate.javaclient.stompclient;


import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventuateSTOMPClientTest {

  private EventuateSTOMPClient client;
  private TrackingStompServer server;
  private EventuateCredentials credentials = new EventuateCredentials("x", "y", "default");
  private static Vertx vertx;
  private static int port;

  @BeforeClass
  public static void beforeClass() throws IOException {
    port = PortUtil.findPort();
    vertx = Vertx.vertx();
  }

  @AfterClass
  public static void afterClass() {
    System.out.println("Closing vertx");
    if (vertx != null) vertx.close();
  }

  @After
  public void tearDown() throws ExecutionException, InterruptedException {
    if (server != null)
      server.close();
  }

  private void makeClient() throws URISyntaxException {
    client = new EventuateSTOMPClient(vertx, credentials, new URI("stomp://localhost:" + port));

    client.subscribe("MySubId",
            Collections.singletonMap("MyEntityType",
                    Collections.singleton("MyEvent")),
            null, se -> CompletableFuture.completedFuture("x"));
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
      System.out.println("Testing !isConnected");
      if (!client.isConnected()) break;
      TimeUnit.MILLISECONDS.sleep(50);
    }
    System.out.println("!isConnected");

    assertFalse(client.isConnected());
  }

  private void assertConnected() throws InterruptedException, ExecutionException {
    for (int i = 0; i < 50; i++) {
      System.out.println("Testing isConnected");
      if (client.isConnected()) break;
      TimeUnit.MILLISECONDS.sleep(100);
    }
    System.out.println("isConnected");

    assertTrue(client.isConnected());
  }


  @Test
  public void shouldConnect() throws InterruptedException, URISyntaxException, IOException, ExecutionException {

    makeServer();

    makeClient();

    assertConnected();

    server.assertSubscribed();

    server.close();

    System.out.println("server closed.. ");

    assertDisconnected();


    makeServer();
    assertConnected();

    server.assertSubscribed();

    System.out.println("Closing client");
    client.close();


    System.out.println("Exiting test");

  }

  @Test
  public void shouldRepeatedlyAttemptToConnect() throws InterruptedException, URISyntaxException, IOException, ExecutionException {

    makeClient();

    TimeUnit.SECONDS.sleep(5);

    makeServer();

    assertConnected();

    server.assertSubscribed();

    server.close();
    client.close();

    System.out.println("server closed.. ");

    System.out.println("Closing client");

    System.out.println("Exiting test");
  }

  @Test
  public void trySubscribing() throws InterruptedException, URISyntaxException, IOException, ExecutionException {

    makeServer();
    makeClient();

    assertConnected();

    // server.assertSubscribed();

    client.subscribe("MySubId2",
            Collections.singletonMap("MyEntityType2",
                    Collections.singleton("MyEvent")),
            null, se -> {
              System.out.print("Y");
              return CompletableFuture.completedFuture("x");
            });

    TimeUnit.SECONDS.sleep(5);

    client.close();

    server.close();

    System.out.println("server closed.. ");

    System.out.println("Closing client");

    System.out.println("Exiting test");
  }


  @Test
  public void shouldClose() throws Exception {

    makeServer();

    makeClient();

    assertConnected();

    server.assertSubscribed();

    client.close();

    server.assertClientIsDisconnected();
  }

  }
