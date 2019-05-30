package io.eventuate.javaclient.restclient;

import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.common.id.generator.Int128;

import java.util.List;

public class UpdateEntityRequest {

  private List<EventTypeAndData> events;
  private Int128 entityVersion;
  private String triggeringEventToken;

  public UpdateEntityRequest(List<EventTypeAndData> events, Int128 entityVersion, String eventToken) {
    this.events = events;
    this.entityVersion = entityVersion;
    this.triggeringEventToken = eventToken;
  }

  public List<EventTypeAndData> getEvents() {
    return events;
  }

  public Int128 getEntityVersion() {
    return entityVersion;
  }

  public String getTriggeringEventToken() {
    return triggeringEventToken;
  }
}

