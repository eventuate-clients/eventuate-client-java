package io.eventuate.javaclient.restclient;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockHttpServer {
  private HttpServer server;
  private  Expectations expectations;

  private CompletableFuture<Object> listenFuture = new CompletableFuture<>();

  public MockHttpServer(Vertx vertx, int port)  {
    server = vertx.createHttpServer().requestHandler(this::requestHandler).listen(port, event -> {
      if (event.failed())
        listenFuture.completeExceptionally(event.cause());
      else
        listenFuture.complete(event.result());
    });
    expectations = new Expectations();
  }

  public void waitUntilListening() {
    try {
      listenFuture.get(5, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
  private void requestHandler(HttpServerRequest httpServerRequest) {
    if (!expectations.hasNext())
      httpServerRequest.response().setStatusCode(500).end();
    else
      expectations.next().handleRequest(httpServerRequest);
  }

  void closeSync() throws InterruptedException, ExecutionException {
    CompletableFuture<Void> f = new CompletableFuture<>();
    server.close(ignored -> f.complete(null));
    f.get();
  }

  public ExpectionBuilder expect() {
    return expectations.expect();
  }

  public void assertSatisfied() {
    expectations.assertSatisfied();
  }
}