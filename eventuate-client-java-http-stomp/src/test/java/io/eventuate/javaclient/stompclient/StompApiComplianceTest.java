package io.eventuate.javaclient.stompclient;

import io.eventuate.SubscriberOptions;
import io.eventuate.javaclient.commonimpl.JSonMapper;
import io.eventuate.Int128;
import io.eventuate.javaclient.restclient.AbstractApiComplianceTest;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;

public class StompApiComplianceTest extends AbstractApiComplianceTest {

  @Test
  public void serializedEventShouldConform() throws IOException {
    String json = getJsonResourceConformingTo("/json/stompEvent.json", "stomp/stomp-event.json");
    StompEvent se = JSonMapper.fromJson(json, StompEvent.class);
    assertEquals(Int128.fromString("000001539af9df31-0242ac1100570002"), se.getId());
    assertEquals("000001539af9db21-0242ac11003d0000", se.getEntityId());
    assertEquals("net.chrisrichardson.eventstore.client.javaintegrationtests.domain.Account", se.getEntityType());
    assertEquals("{\"amount\":1,\"transactionId\":\"000001539af9de13-0242ac1100800000\"}", se.getEventData());
    assertEquals("net.chrisrichardson.eventstore.client.javaintegrationtests.domain.AccountDebitedEvent", se.getEventType());
  }


  @Test
  public void subscriptionIdWithSpaceShouldConform() throws IOException {
    shouldConformTo(new SubscriptionIdWithSpace("MySubId", null, Collections.singletonMap("MyAggregateType", Collections.singleton("MyEvent")), null), "stomp/destination-header.json");
  }

  @Test
  public void subscriptionIdWithSpaceWithOptionsShouldConform() throws IOException {
    shouldConformTo(new SubscriptionIdWithSpace("MySubId", null, Collections.singletonMap("MyAggregateType", Collections.singleton("MyEvent")), SubscriberOptions.DEFAULTS), "stomp/destination-header.json");
  }
}
