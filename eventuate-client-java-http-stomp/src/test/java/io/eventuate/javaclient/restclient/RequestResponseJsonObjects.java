package io.eventuate.javaclient.restclient;

import io.eventuate.EventContext;
import io.eventuate.common.id.Int128;
import org.json.JSONObject;

import java.util.Collections;

public class RequestResponseJsonObjects {

  public static final String ENTITY_ID = "XXX";
  static final String aggregateType = "io.eventuate.javaclient.testdomain.Account";
  static final String createdEvent = "io.eventuate.javaclient.testdomain.AccountCreatedEvent";
  static final String debitedEvent = "io.eventuate.javaclient.testdomain.AccountDebitedEvent";
  static final String eventData = "{}";
  static String createId = "CreateId";

  static JSONObject makeCreateResponse() {
    return new JSONObject().put("entityId", ENTITY_ID).put("entityVersion", "1-2").put("eventIds", Collections.singletonList("3-4"));
  }

  static JSONObject makeExpectedCreateRequest() {
    return new JSONObject()
            .put("entityTypeName", aggregateType)
            .put("events", Collections.singletonList(new JSONObject().put("eventType", createdEvent).put("eventData", eventData)));
  }

  static JSONObject makeCreateWithIdResponse() {
    return new JSONObject().put("entityId", createId).put("entityVersion", "1-2").put("eventIds", Collections.singletonList("3-4"));
  }

  static JSONObject makeExpectedCreateWithIdRequest() {
    return makeExpectedCreateRequest()
            .put("entityId", createId);
  }

  static JSONObject makeEntityExistsErrorResponse() {
    return new JSONObject().put("error", "entity_exists").put("explanation", "Entity already exists");
  }

  static JSONObject makeExpectedUpdateRequest() {
    return new JSONObject()
            .put("entityVersion", new Int128(5,6).asString())
            .put("events", Collections.singletonList(new JSONObject().put("eventType", debitedEvent).put("eventData", eventData)));
  }

  static JSONObject makeOptimisticLockingErrorResponse() {
    return new JSONObject().put("error", "optimistic_lock_error").put("explanation", "optimistic locking failure");
  }

  static JSONObject makeFindResponse() {
    return new JSONObject().put("events",
            Collections.singletonList(new JSONObject()
                    .put("id", "8-9")
                    .put("eventType", createdEvent)
                    .put("eventData", eventData)
            ));
  }

  static JSONObject makeFindNonExistentResponse() {
    return new JSONObject().put("events", Collections.emptyList());
  }

  public static JSONObject makeDuplicateEventErrorResponse() {
    return new JSONObject().put("error", "duplicate_event").put("explanation", "duplicateEvent");
  }

  public static JSONObject makeExpectedUpdateRequestWithTriggeringEvent() {
    return makeExpectedUpdateRequest().put("triggeringEventToken", "myeventtoken");
  }

  static EventContext makeEventContext() {
    return new EventContext("myeventtoken");
  }
}
