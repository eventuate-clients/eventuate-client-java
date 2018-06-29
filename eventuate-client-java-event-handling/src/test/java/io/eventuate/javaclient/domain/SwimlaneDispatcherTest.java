package io.eventuate.javaclient.domain;

import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import io.eventuate.EventContext;
import io.eventuate.Int128;
import io.eventuate.testutil.Eventually;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SwimlaneDispatcherTest {
  private SwimlaneDispatcher swimlaneDispatcher;
  private AtomicInteger numberOfEventsReceived;
  private Function<DispatchedEvent<Event>, CompletableFuture<?>> handler;

  @Before
  public void init() {
    swimlaneDispatcher = new SwimlaneDispatcher("1", 1, Executors.newCachedThreadPool());
    numberOfEventsReceived = new AtomicInteger(0);
  }

  @Test
  public void shouldDispatchManyEvents() {
    int numberOfEventsToSend = 5;

    createHandler();

    sendEvents(numberOfEventsToSend);
    assertEventsReceived(numberOfEventsToSend);
  }

  @Test
  public void testShouldRestart() {
    int numberOfEventsToSend = 5;

    createHandler();

    sendEvents(numberOfEventsToSend);
    assertDispatcherStopped();
    sendEvents(numberOfEventsToSend);
    assertEventsReceived(numberOfEventsToSend * 2);
  }

  private void createHandler() {
    handler = evnt ->
      CompletableFuture.supplyAsync(() -> {
        numberOfEventsReceived.incrementAndGet();
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        return null;
      });
  }

  private void sendEvents(int numberOfEventsToSend) {
    for (int i = 0; i < numberOfEventsToSend; i++) {
      if (i > 0) {
        Assert.assertTrue(swimlaneDispatcher.getRunning());
      }
      swimlaneDispatcher.dispatch(new DispatchedEvent<>("1", new Int128(1, 1), new Event() {}, 1, 1L, new EventContext(), Optional.empty()), handler);
    }
  }

  private void assertEventsReceived(int numberOfEventsToSend) {
    Eventually.eventually(() -> Assert.assertEquals(numberOfEventsToSend, numberOfEventsReceived.get()));
  }

  private void assertDispatcherStopped() {
    Eventually.eventually(() -> Assert.assertFalse(swimlaneDispatcher.getRunning()));
  }
}
