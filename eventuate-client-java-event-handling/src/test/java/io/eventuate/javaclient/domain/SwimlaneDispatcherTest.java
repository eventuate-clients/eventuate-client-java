package io.eventuate.javaclient.domain;

import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import io.eventuate.EventContext;
import io.eventuate.Int128;
import io.eventuate.testutil.Eventually;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class SwimlaneDispatcherTest {
  private SwimlaneDispatcher swimlaneDispatcher;
  private Function<DispatchedEvent<Event>, CompletableFuture<?>> handler;
  private Set<String> dataReturnedByHandlerFutures;
  private Set<String> dataReceivedFromDispatcherFutures;
  private Set<String> throwableDataReturnedByHandlerFutures;
  private Set<String> throwableDataReceivedFromDispatcherFutures;

  @Before
  public void init() {
    dataReturnedByHandlerFutures = new HashSet<>();
    dataReceivedFromDispatcherFutures = new HashSet<>();
    throwableDataReturnedByHandlerFutures = new HashSet<>();
    throwableDataReceivedFromDispatcherFutures = new HashSet<>();

    swimlaneDispatcher = new SwimlaneDispatcher("1", 1, Executors.newCachedThreadPool());
  }

  @Test
  public void shouldDispatchManyEvents() {
    int numberOfEventsToSend = 5;

    createHandler(false);

    sendEvents(numberOfEventsToSend);
    assertEventsReceived(numberOfEventsToSend);
  }

  @Test
  public void shouldHandleFailures() {
    int numberOfEventsToSend = 5;

    createHandler(true);

    sendEvents(numberOfEventsToSend);
    assertEventsFailed(numberOfEventsToSend);
  }

  @Test
  public void testShouldRestart() {
    int numberOfEventsToSend = 5;

    createHandler(false);

    sendEvents(numberOfEventsToSend);
    assertDispatcherStopped();
    sendEvents(numberOfEventsToSend);
    assertEventsReceived(numberOfEventsToSend * 2);
  }

  private void createHandler(boolean shouldFail) {
    handler = event -> {

      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      return CompletableFuture.supplyAsync(() -> {
        String data = UUID.randomUUID().toString();

        if (shouldFail) {
          throwableDataReturnedByHandlerFutures.add(data);
          throw new RuntimeException(data);
        }

        dataReturnedByHandlerFutures.add(data);
        return data;
      });
    };
  }

  private void sendEvents(int numberOfEventsToSend) {
    for (int i = 0; i < numberOfEventsToSend; i++) {
      if (i > 0) {
        Assert.assertTrue(swimlaneDispatcher.getRunning());
      }
      CompletableFuture<String> future = (CompletableFuture<String>)swimlaneDispatcher.dispatch(new DispatchedEvent<>("1",
              new Int128(1, 1),
              new Event() {},
              1,
              1L,
              new EventContext(),
              Optional.empty()), handler);

      future.whenCompleteAsync((data, throwable) -> {
        if (throwable != null) {
          throwableDataReceivedFromDispatcherFutures.add(throwable.getCause().getMessage());
        } else {
          dataReceivedFromDispatcherFutures.add(data);
        }
      });
    }
  }

  private void assertEventsReceived(int numberOfEventsToSend) {
    Eventually.eventually(() -> {
      Assert.assertTrue(throwableDataReturnedByHandlerFutures.isEmpty());
      Assert.assertTrue(throwableDataReceivedFromDispatcherFutures.isEmpty());

      Assert.assertEquals(numberOfEventsToSend, dataReturnedByHandlerFutures.size());
      Assert.assertEquals(numberOfEventsToSend, dataReceivedFromDispatcherFutures.size());
      Assert.assertEquals(dataReturnedByHandlerFutures, dataReceivedFromDispatcherFutures);
    });
  }

  private void assertEventsFailed(int numberOfEventsToSend) {
    Eventually.eventually(() -> {
      Assert.assertTrue(dataReceivedFromDispatcherFutures.isEmpty());
      Assert.assertTrue(dataReturnedByHandlerFutures.isEmpty());

      Assert.assertEquals(numberOfEventsToSend, throwableDataReturnedByHandlerFutures.size());
      Assert.assertEquals(numberOfEventsToSend, throwableDataReceivedFromDispatcherFutures.size());
      Assert.assertEquals(throwableDataReturnedByHandlerFutures, throwableDataReceivedFromDispatcherFutures);
    });
  }

  private void assertDispatcherStopped() {
    Eventually.eventually(() -> Assert.assertFalse(swimlaneDispatcher.getRunning()));
  }
}
