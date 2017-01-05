package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.javaclient.commonimpl.EventIdTypeAndData;

public class EventAndTrigger {

  public final EventIdTypeAndData event;
  public final String triggeringEvent;

  public EventAndTrigger(EventIdTypeAndData event, String triggeringEvent) {

    this.event = event;
    this.triggeringEvent = triggeringEvent;
  }
}
