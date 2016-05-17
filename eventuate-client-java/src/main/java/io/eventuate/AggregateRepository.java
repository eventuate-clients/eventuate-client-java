package io.eventuate;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AggregateRepository<T extends CommandProcessingAggregate<T, CT>, CT extends Command> {

  private static Logger logger = LoggerFactory.getLogger(AggregateRepository.class);

  private Class<T> clasz;
  private EventuateAggregateStore aggregateStore;

  public AggregateRepository(Class<T> clasz, EventuateAggregateStore aggregateStore) {
    this.clasz = clasz;
    this.aggregateStore = aggregateStore;
  }


  public <CT2 extends CT> CompletableFuture<EntityWithIdAndVersion<T>> save(CT2 cmd) {
    return save(cmd, Optional.empty());
  }

  public <CT2 extends CT> CompletableFuture<EntityWithIdAndVersion<T>> save(CT2 cmd, Optional<SaveOptions> saveOptions) {
    T aggregate;
    try {
      aggregate = clasz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    List<Event> events = aggregate.processCommand(cmd);
    Aggregates.applyEventsToMutableAggregate(aggregate, events);

    return aggregateStore.save(clasz, events, saveOptions).thenApply(entityIdAndVersion -> new EntityWithIdAndVersion<>(entityIdAndVersion, aggregate));
  }

  public CompletableFuture<EntityWithIdAndVersion<T>> update(String entityId, final CT cmd) {
    return update(entityId, cmd, Optional.empty());
  }

  class LoadedEntityWithMetadata {
    boolean success;
    EntityWithMetadata<T> ewmd;

    LoadedEntityWithMetadata(boolean success, EntityWithMetadata<T> ewmd) {
      this.success = success;
      this.ewmd = ewmd;
    }
  }

  private <T> CompletableFuture<T> withRetry(Supplier<CompletableFuture<T>> asyncRequest) {
    CompletableFuture<T> result = new CompletableFuture<>();
    attemptOperation(asyncRequest, result);
    return result;
  }

  private <T> void attemptOperation(Supplier<CompletableFuture<T>> asyncRequest, CompletableFuture<T> result) {
    CompletableFuture<T> f = asyncRequest.get();
    f.handleAsync((val, throwable) -> {
      if (throwable != null) {
        if (CompletableFutureUtil.unwrap(throwable) instanceof OptimisticLockingException) {
          attemptOperation(asyncRequest, result);
        } else
          result.completeExceptionally(throwable);
      }
      else
        result.complete(val);
      return null;
    });
  }


  public CompletableFuture<EntityWithIdAndVersion<T>> update(final String entityId, final CT cmd, Optional<UpdateOptions> updateOptions) {

    return withRetry( () -> {
      CompletableFuture<LoadedEntityWithMetadata> eo = aggregateStore.find(clasz, entityId, updateOptions.map(uo -> new FindOptions().withTriggeringEvent(uo.getTriggeringEvent())))
              .handleAsync((tEntityWithMetadata, throwable) -> {
                if (throwable == null)
                  return new LoadedEntityWithMetadata(true, tEntityWithMetadata);
                else {
                  logger.debug("Exception finding aggregate", throwable);
                  Throwable unwrapped = CompletableFutureUtil.unwrap(throwable);
                  if (unwrapped instanceof DuplicateTriggeringEventException)
                    return new LoadedEntityWithMetadata(false, null);
                  else if (unwrapped instanceof RuntimeException)
                    throw (RuntimeException) unwrapped;
                  else if (throwable instanceof RuntimeException)
                    throw (RuntimeException) throwable;
                  else
                    // TODO - does this make sense?
                    throw new RuntimeException(throwable);
                }
              });

      return eo.thenCompose(loadedEntityWithMetadata -> {
        if (loadedEntityWithMetadata.success) {
          EntityWithMetadata<T> entityWithMetaData = loadedEntityWithMetadata.ewmd;
          final T aggregate = entityWithMetaData.getEntity();
          List<Event> events = aggregate.processCommand(cmd);
          if (events.isEmpty()) {
            return CompletableFuture.completedFuture(entityWithMetaData.toEntityWithIdAndVersion());
          } else {
            return aggregateStore.update(clasz, entityWithMetaData.getEntityIdAndVersion(), events, updateOptions)
                    .thenApply(entityIdAndVersion -> {
                      Aggregates.applyEventsToMutableAggregate(aggregate, events);
                      return new EntityWithIdAndVersion<T>(entityIdAndVersion, aggregate);
                    })
                    .handle((r, t) -> {
                      if (t == null)
                        return r;
                      else {
                        logger.debug("Exception updating aggregate", t);
                        Throwable unwrapped = CompletableFutureUtil.unwrap(t);
                        if (unwrapped instanceof DuplicateTriggeringEventException)
                          return new EntityWithIdAndVersion<T>(entityWithMetaData.getEntityIdAndVersion(), aggregate);
                        else if (unwrapped instanceof RuntimeException)
                          throw (RuntimeException) unwrapped;
                        else if (t instanceof RuntimeException)
                          throw (RuntimeException) t;
                        else
                          throw new RuntimeException(unwrapped);
                      }
                    });
          }
        } else {
          return aggregateStore.find(clasz, entityId, Optional.empty()).thenApply(EntityWithMetadata::toEntityWithIdAndVersion);
        }
      });
    });
  }


}

