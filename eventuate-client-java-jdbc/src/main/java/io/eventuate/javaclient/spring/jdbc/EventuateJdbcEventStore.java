package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.*;
import io.eventuate.javaclient.commonimpl.*;
import io.eventuate.DuplicateTriggeringEventException;
import io.eventuate.OptimisticLockingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EventuateJdbcEventStore extends AbstractEventuateJdbcAggregateStore
        implements AggregateCrud, AggregateEvents {


  private AtomicLong eventOffset = new AtomicLong();

  public EventuateJdbcEventStore(JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  protected void publish(String aggregateType, String aggregateId, List<EventIdTypeAndData> eventsWithIds) {
    synchronized (aggregateTypeToSubscription) {
      List<Subscription> subscriptions = aggregateTypeToSubscription.get(aggregateType);
      if (subscriptions != null)
        for (Subscription subscription : subscriptions) {
          for (EventIdTypeAndData event : eventsWithIds) {
            if (subscription.isInterestedIn(aggregateType, event.getEventType()))
              subscription.handler.apply(new SerializedEvent(event.getId(), aggregateId, aggregateType, event.getEventData(), event.getEventType(),
                      aggregateId.hashCode() % 8,
                      eventOffset.getAndIncrement(),
                      new EventContext(event.getId().asString())));
          }
        }
    }

  }

  private final Map<String, List<Subscription>> aggregateTypeToSubscription = new HashMap<>();

  class Subscription {

    private final String subscriberId;
    private final Map<String, Set<String>> aggregatesAndEvents;
    private final Function<SerializedEvent, CompletableFuture<?>> handler;

    public Subscription(String subscriberId, Map<String, Set<String>> aggregatesAndEvents, Function<SerializedEvent, CompletableFuture<?>> handler) {

      this.subscriberId = subscriberId;
      this.aggregatesAndEvents = aggregatesAndEvents;
      this.handler = handler;
    }

    public boolean isInterestedIn(String aggregateType, String eventType) {
      return aggregatesAndEvents.get(aggregateType) != null && aggregatesAndEvents.get(aggregateType).contains(eventType);
    }
  }

  @Override
  public CompletableFuture<?> subscribe(String subscriberId, Map<String, Set<String>> aggregatesAndEvents, SubscriberOptions options, Function<SerializedEvent, CompletableFuture<?>> handler) {
    // TODO handle options
    Subscription subscription = new Subscription(subscriberId, aggregatesAndEvents, handler);
    synchronized (aggregateTypeToSubscription) {
      for (String aggregateType : aggregatesAndEvents.keySet()) {
        List<Subscription> existing = aggregateTypeToSubscription.get(aggregateType);
        if (existing == null) {
          existing = new LinkedList<>();
          aggregateTypeToSubscription.put(aggregateType, existing);
        }
        existing.add(subscription);
      }
    }
    return CompletableFuture.completedFuture(null);
  }

}
