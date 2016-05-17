package io.eventuate;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface EventHandlerContext<T extends Event> extends EventEnvelope<T> {

  T getEvent();

  <U extends CommandProcessingAggregate<U, CT>, CT extends Command> CompletableFuture<EntityWithIdAndVersion<U>> save(Class<U>  entityClass, CT command, Optional<String> entityId);
  <U extends CommandProcessingAggregate<U, CT>, CT extends Command> CompletableFuture<EntityWithIdAndVersion<U>> update(Class<U>  entityClass, String entityId, CT command);

}
