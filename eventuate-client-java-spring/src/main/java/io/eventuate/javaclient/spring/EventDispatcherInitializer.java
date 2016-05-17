package io.eventuate.javaclient.spring;

import io.eventuate.*;
import io.eventuate.EventuateSubscriptionFailedException;
import io.eventuate.javaclient.domain.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class EventDispatcherInitializer {

  private EventHandlerProcessor[] processors;
  private EventuateAggregateStore aggregateStore;
  private Executor executorService;

  private Set<String> subscriberIds = new HashSet<>();

  public EventDispatcherInitializer(EventHandlerProcessor[] processors, EventuateAggregateStore aggregateStore, Executor executorService) {
    this.processors = processors;
    this.aggregateStore = aggregateStore;
    this.executorService = executorService;
  }

  public void registerEventHandler(Object eventHandlerBean, String beanName) {

    List<EventHandler> handlers = Arrays.stream(ReflectionUtils.getUniqueDeclaredMethods(eventHandlerBean.getClass()))
            .filter(method -> method.getAnnotation(EventHandlerMethod.class) != null)
            .map(method -> Arrays.stream(processors).filter(processor -> processor.supports(method)).findFirst().orElseThrow(() -> new RuntimeException("Don't know what to do with method " + method))
                    .process(eventHandlerBean, method))
            .collect(Collectors.toList());

    Map<String, Set<String>> aggregatesAndEvents = makeAggregatesAndEvents(handlers.stream()
            .filter(handler -> !handler.getEventType().equals(EndOfCurrentEventsReachedEvent.class)).collect(Collectors.toList()));

    Map<Class<?>, EventHandler> eventTypesAndHandlers = makeEventTypesAndHandlers(handlers);

    EventSubscriber a = AnnotationUtils.findAnnotation(eventHandlerBean.getClass(), EventSubscriber.class);
    if (a == null)
      throw new RuntimeException("Needs @EventSubscriber annotation: " + eventHandlerBean);

    String subscriberId = StringUtils.isBlank(a.id()) ?  beanName : a.id();

    EventDispatcher eventDispatcher = new EventDispatcher(subscriberId, eventTypesAndHandlers);

    SwimlaneBasedDispatcher swimlaneBasedDispatcher = new SwimlaneBasedDispatcher(subscriberId, executorService);


    if (subscriberIds.contains(subscriberId))
      throw new RuntimeException("Duplicate subscriptionId " + subscriberId);
    subscriberIds.add(subscriberId);

    SubscriberOptions subscriberOptions = new SubscriberOptions(a.durability(), a.readFrom(), a.progressNotifications());

    // TODO - it would be nice to do this in parallel
    try {
      aggregateStore.subscribe(subscriberId, aggregatesAndEvents,
              subscriberOptions, de -> swimlaneBasedDispatcher.dispatch(de, eventDispatcher::dispatch)).get(20, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      throw new EventuateSubscriptionFailedException(subscriberId, e);
    }
  }

  private Map<Class<?>, EventHandler> makeEventTypesAndHandlers(List<EventHandler> handlers) {
      return handlers.stream().collect(Collectors.toMap(EventHandler::getEventType, eh -> eh));

  }

  private Map<String, Set<String>> makeAggregatesAndEvents(List<EventHandler> handlers) {
    return handlers.stream().collect(Collectors.toMap(
            eh -> EventEntityUtil.toEntityTypeName(eh.getEventType()),
            eh -> Collections.singleton(eh.getEventType().getName()),
            (e1, e2)  -> {
              HashSet<String> r = new HashSet<String>(e1);
              r.addAll(e2);
              return r;
            }
            ));
  }

}
