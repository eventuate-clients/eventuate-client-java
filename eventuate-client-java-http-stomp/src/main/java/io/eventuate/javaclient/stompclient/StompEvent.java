package io.eventuate.javaclient.stompclient;

import io.eventuate.common.id.Int128;
import org.apache.commons.lang.builder.ToStringBuilder;

public class StompEvent {
  private Int128 id;
  private String entityId;
  private String entityType;
  private String eventData;
  private String eventType;
  private String eventToken;
  private Integer swimlane;
  private Long offset;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public Int128 getId() {
    return id;
  }

  public void setId(Int128 id) {
    this.id = id;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getEventData() {
    return eventData;
  }

  public void setEventData(String eventData) {
    this.eventData = eventData;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getEventToken() {
    return eventToken;
  }

  public void setEventToken(String eventToken) {
    this.eventToken = eventToken;
  }

  public Integer getSwimlane() {
    return swimlane;
  }

  public void setSwimlane(Integer swimlane) {
    this.swimlane = swimlane;
  }

  public Long getOffset() {
    return offset;
  }

  public void setOffset(Long offset) {
    this.offset = offset;
  }
}
