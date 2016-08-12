package io.eventuate.javaclient.stompclient;


import io.eventuate.Int128;
import io.vertx.core.Vertx;
import org.junit.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class EventuateSTOMPClientTest {

  private EventuateSTOMPClient client;
  private TrackingStompServer server;
  private EventuateCredentials credentials = new EventuateCredentials("x", "y", "default");
  private static Vertx vertx;
  private static int port;
  private BlockingQueue<Int128> ids;

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

  private void makeClient() throws URISyntaxException {
    client = new EventuateSTOMPClient(vertx, credentials, new URI("stomp://localhost:" + port));

    client.subscribe("MySubId",
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

    makeClient();

    assertConnected();

    server.assertSubscribed();

    List<Int128> receivedIds = new ArrayList<>();

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
  public void shouldRepeatedlyAttemptToConnect() throws InterruptedException, URISyntaxException, IOException, ExecutionException {

    makeClient();

    TimeUnit.SECONDS.sleep(5);

    makeServer();

    assertConnected();

    server.assertSubscribed();

    server.close();
    client.close();

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
