package io.eventuate.javaclient.restclient;

import io.eventuate.EntityIdAndType;
import io.eventuate.Int128;
import io.eventuate.OptimisticLockingException;
import io.eventuate.javaclient.commonimpl.EntityIdVersionAndEventIds;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;
import io.eventuate.javaclient.stompclient.EventuateCredentials;
import io.eventuate.javaclient.stompclient.PortUtil;
import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.fail;

public class EventuateRESTClientTest {
  private static EventuateCredentials credentials = new EventuateCredentials("x", "y", "default");
  private static Vertx vertx;
  private static EventuateRESTClient client;
  private static int port;
  private MockHttpServer mockHttpServer;

  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException {
    port = PortUtil.findPort();
    vertx = Vertx.vertx();
    client = new EventuateRESTClient(vertx, credentials, new URI("http://localhost:" + port));
  }

  @Before
  public void before() {
    mockHttpServer = new MockHttpServer(vertx, port);
  }

  @After
  public void after() throws ExecutionException, InterruptedException {
    if (mockHttpServer != null) {
      mockHttpServer.closeSync();
    }
    mockHttpServer = null;
  }


  @Test
  public void shouldRetryAfterServiceUnavailable() throws InterruptedException, ExecutionException, TimeoutException {
    mockHttpServer.expect().respondingWith(503);
    mockHttpServer.expect().respondingWith(200, RequestResponseJsonObjects.makeFindResponse());


    CompletableFuture<LoadedEvents> f = client.find(RequestResponseJsonObjects.aggregateType, RequestResponseJsonObjects.ENTITY_ID, Optional.empty());
    f.get(4, TimeUnit.SECONDS);

    mockHttpServer.assertSatisfied();
  }

  @Test
  public void shouldHandleErrorAfterServiceUnavailable() throws InterruptedException, ExecutionException, TimeoutException {
    mockHttpServer.expect().respondingWith(503);
    mockHttpServer.expect().respondingWith(409, RequestResponseJsonObjects.makeOptimisticLockingErrorResponse());


    CompletableFuture<EntityIdVersionAndEventIds> f = client.update(new EntityIdAndType(RequestResponseJsonObjects.ENTITY_ID, RequestResponseJsonObjects.aggregateType),
            new Int128(5, 6),
            Collections.singletonList(new EventTypeAndData(RequestResponseJsonObjects.debitedEvent, RequestResponseJsonObjects.eventData, Optional.empty())), Optional.empty());
    try {
      f.get(4, TimeUnit.SECONDS);
      fail();
    } catch (ExecutionException e) {
      if (!(e.getCause() instanceof OptimisticLockingException))
        throw e;
    }

    mockHttpServer.assertSatisfied();
  }


}
