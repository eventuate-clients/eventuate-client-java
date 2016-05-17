package io.eventuate;

import java.util.List;

public interface CommandProcessingAggregate<U extends CommandProcessingAggregate, CT>  extends Aggregate<U> {
  List<Event> processCommand(CT cmd);
}
