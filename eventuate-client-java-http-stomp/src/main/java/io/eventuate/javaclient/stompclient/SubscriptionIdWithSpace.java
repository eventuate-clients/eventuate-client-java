package io.eventuate.javaclient.stompclient;


import io.eventuate.SubscriberOptions;

import java.util.Map;
import java.util.Set;

public class SubscriptionIdWithSpace {
  private String subscriberId;
  private String space = null;
  private Map<String, Set<String>> entityTypesAndEvents;
  private SubscriberOptions options;

  public SubscriptionIdWithSpace(String subscriberId, String space, Map<String, Set<String>> entityTypesAndEvents, SubscriberOptions options) {
    this.subscriberId = subscriberId;
    this.space = space;
    this.entityTypesAndEvents = entityTypesAndEvents;
    this.options = options;
  }

  public String getSubscriberId() {
    return subscriberId;
  }

  public Map<String, Set<String>> getEntityTypesAndEvents() {
    return entityTypesAndEvents;
  }

  public String getSpace() {
    return space;
  }

  public SubscriberOptions getOptions() {
    return options;
  }
}
