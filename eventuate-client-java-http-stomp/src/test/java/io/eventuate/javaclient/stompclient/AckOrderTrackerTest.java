package io.eventuate.javaclient.stompclient;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class AckOrderTrackerTest {

  @Test
  public void shouldReorder() {
    AckOrderTracker ackOrderTracker = new AckOrderTracker();

    ackOrderTracker.add("a");
    ackOrderTracker.add("b");
    ackOrderTracker.add("c");

    assertEquals(Collections.emptyList(), ackOrderTracker.ack("b"));
    assertEquals(Arrays.asList("a", "b"), ackOrderTracker.ack("a"));
    assertEquals(Collections.singletonList("c"), ackOrderTracker.ack("c"));
  }

  @Test
  public void shouldWork() {
    AckOrderTracker ackOrderTracker = new AckOrderTracker();

    ackOrderTracker.add("a");
    ackOrderTracker.add("b");
    ackOrderTracker.add("c");

    assertEquals(Collections.singletonList("a"), ackOrderTracker.ack("a"));
    assertEquals(Collections.singletonList("b"), ackOrderTracker.ack("b"));
    assertEquals(Collections.singletonList("c"), ackOrderTracker.ack("c"));
  }

  @Test
  public void shouldWork1() {
    AckOrderTracker ackOrderTracker = new AckOrderTracker();

    ackOrderTracker.add("a");
    ackOrderTracker.add("b");
    ackOrderTracker.add("c");

    assertEquals(Collections.emptyList(), ackOrderTracker.ack("c"));
    assertEquals(Collections.emptyList(), ackOrderTracker.ack("b"));
    assertEquals(Arrays.asList("a", "b", "c"), ackOrderTracker.ack("a"));
    assertTrue(ackOrderTracker.pendingHeaders.isEmpty());
  }

  @Test
  public void shouldWork2() {
    AckOrderTracker ackOrderTracker = new AckOrderTracker();

    ackOrderTracker.add("a");
    ackOrderTracker.add("b");
    ackOrderTracker.add("c");

    assertEquals(Collections.emptyList(), ackOrderTracker.ack("c"));
    assertEquals(Collections.singletonList("a"), ackOrderTracker.ack("a"));
    assertEquals(Arrays.asList("b", "c"), ackOrderTracker.ack("b"));
    assertTrue(ackOrderTracker.pendingHeaders.isEmpty());
  }

}