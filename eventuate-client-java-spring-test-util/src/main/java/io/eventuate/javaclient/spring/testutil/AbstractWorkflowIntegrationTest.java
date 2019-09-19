package io.eventuate.javaclient.spring.testutil;

import io.eventuate.RegisteredSubscription;
import io.eventuate.SubscriptionsRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractWorkflowIntegrationTest {
  @Autowired
  private SubscriptionsRegistry subscriptionsRegistry;

  protected void assertSubscribedToAggregateEvents(RegisteredSubscription registeredSubscription, String aggregateType, String... eventTypes) {
    assertEquals(asSet(eventTypes), registeredSubscription.getAggregatesAndEvents().get(aggregateType));
  }

  protected void assertSubscribedToAggregates(RegisteredSubscription registeredSubscription, String... aggregateTypes) {
    assertEquals(asSet(aggregateTypes), registeredSubscription.getAggregatesAndEvents().keySet());
  }

  protected void assertEventHandlerClass(Class<?> eventHandlerClass, RegisteredSubscription registeredSubscription) {
    assertTrue("Not correct type: " + registeredSubscription.getEventSubscriberClass(),
            eventHandlerClass.isAssignableFrom(registeredSubscription.getEventSubscriberClass()));
  }

  protected RegisteredSubscription getRegisteredSubscription(String subscriberId) {
    return getRegisteredSubscriptions().stream().filter(rs -> rs.getSubscriberId().equals(subscriberId))
            .findFirst().orElseThrow(() -> new RuntimeException("cannot find subscription: " + subscriberId));
  }

  protected void assertSubscriptionIds(String... subscriptionIds) {
    assertEquals(asSet(subscriptionIds), getRegisteredSubscriptions().stream().map(RegisteredSubscription::getSubscriberId).collect(toSet()));
  }

  private Set<String> asSet(String... xs) {
    return Arrays.stream(xs).collect(toSet());
  }

  private List<RegisteredSubscription> getRegisteredSubscriptions() {
    return subscriptionsRegistry.getRegisteredSubscriptions();
  }
}
