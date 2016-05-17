package io.eventuate.javaclient.commonimpl;

import io.eventuate.javaclient.commonimpl.EventIdTypeAndData;

import java.util.List;

public class LoadedEvents {

  private List<EventIdTypeAndData> events;

  public LoadedEvents(List<EventIdTypeAndData> events) {
    this.events = events;
  }

  public List<EventIdTypeAndData> getEvents() {
    return events;
  }
}
