package io.eventuate;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ReflectiveMutableCommandProcessingAggregate<T extends ReflectiveMutableCommandProcessingAggregate<T, CT>, CT> implements CommandProcessingAggregate<T, CT> {

  @Override
  public T applyEvent(Event event) {
    try {
      getClass().getMethod("apply", event.getClass()).invoke(this, event);
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    return (T)this;
  }

  @Override
  public List<Event> processCommand(CT cmd) {
    try {
      return (List<Event>) getClass().getMethod("process", cmd.getClass()).invoke(this, cmd);
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
