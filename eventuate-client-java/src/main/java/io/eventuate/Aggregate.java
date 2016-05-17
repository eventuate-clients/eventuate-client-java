package io.eventuate;

public interface Aggregate<T extends Aggregate> {

  T applyEvent(Event event);
}