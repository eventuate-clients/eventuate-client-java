package io.eventuate.javaclient.restclient;

import io.eventuate.javaclient.commonimpl.EventIdTypeAndData;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.common.id.generator.Int128;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RestApiComplianceTest extends AbstractApiComplianceTest {


  List<EventTypeAndData> events = Collections.singletonList(new EventTypeAndData("MyEventType", "MyEventData", Optional.empty()));

  @Test
  public void createEntityRequestShouldConform() throws FileNotFoundException {
    shouldConformTo(new CreateEntityRequest("MyEntityTypeName", events), "create-request.json");
  }

  @Test
  public void createEntityResponseShouldConform() throws FileNotFoundException {
    shouldConformTo(new CreateEntityResponse("MyEntityId", Int128.fromString("1-1"), Collections.singletonList(Int128.fromString("2-2"))), "create-response.json");
  }

  @Test
  public void updateEntityRequestShouldConform() throws FileNotFoundException {
    shouldConformTo(new UpdateEntityRequest(events, Int128.fromString("3-3"), null), "update-request.json");
  }

  @Test
  public void updateEntityRequestWithEventTokenShouldConform() throws FileNotFoundException {
    shouldConformTo(new UpdateEntityRequest(events, Int128.fromString("3-3"), "SomeEventToken"), "update-request.json");
  }


  @Test
  public void updateEntityResponseShouldConform() throws FileNotFoundException {
    shouldConformTo(new UpdateEntityResponse("MyEntityId", Int128.fromString("4-4"), Collections.singletonList(Int128.fromString("2-2"))), "update-response.json");
  }

  @Test
  public void getEntityResponseShouldConform() throws FileNotFoundException {
    shouldConformTo(new GetEntityResponse(Collections.singletonList(new EventIdTypeAndData(Int128.fromString("1-1"), "EventType", "EventData", Optional.empty()))), "get-response.json");
  }
}
