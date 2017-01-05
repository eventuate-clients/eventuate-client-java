package io.eventuate;

import java.util.List;

public class Aggregates {
  public static <T extends Aggregate<T>> T applyEventsToMutableAggregate(T aggregate, List<Event> events) {
    for (Event event : events) {
      aggregate = aggregate.applyEvent(event);
    }
    return aggregate;
  }

  public static <T extends Aggregate<T>> T recreateAggregate(Class<T> clasz, List<Event> events) {
    return applyEventsToMutableAggregate(newAggregate(clasz), events);
  }

  private static <T extends Aggregate<T>> T newAggregate(Class<T> clasz) {
    try {
      return clasz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
