package io.eventuate.testutil;

import io.eventuate.CommandProcessingAggregate;
import io.eventuate.EntityWithIdAndVersion;

public interface AggregateOperationInvocation<T extends CommandProcessingAggregate<T, CT>, CT, C extends CT> {
  EntityWithIdAndVersion<T> getEntity();

  C getCommand();
}
