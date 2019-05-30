package io.eventuate.javaclient.restclient;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import io.eventuate.DuplicateTriggeringEventException;
import io.eventuate.EntityAlreadyExistsException;
import io.eventuate.EntityIdAndType;
import io.eventuate.EntityNotFoundException;
import io.eventuate.EventuateServerException;
import io.eventuate.common.id.generator.Int128;
import io.eventuate.OptimisticLockingException;
import io.eventuate.javaclient.commonimpl.AggregateCrudFindOptions;
import io.eventuate.javaclient.commonimpl.AggregateCrudSaveOptions;
import io.eventuate.javaclient.commonimpl.AggregateCrudUpdateOptions;
import io.eventuate.javaclient.commonimpl.EntityIdVersionAndEventIds;
import io.eventuate.javaclient.commonimpl.EventIdTypeAndData;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;
import io.eventuate.javaclient.stompclient.EventuateCredentials;
import io.vertx.core.Vertx;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class EventuateRESTClientPactTest {

  private Vertx vertx = Vertx.vertx();
  private EventuateCredentials credentials = new EventuateCredentials("foo", "bar", "default");
  private EventuateRESTClient client;

  @Before
  public void setUp() throws URISyntaxException {
    if (client == null)
      client = new EventuateRESTClient(vertx, credentials, new URI("http://localhost:8080"));
  }

  @Rule
  public PactProviderRule mockProvider = new PactProviderRule("test_provider", "localhost", 8080, this);


  @Pact(consumer="test_consumer")
  public PactFragment create(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("Create request")
            .path("/entity/default")
            .method("POST")
            .matchHeader("Content-Type", "application/json")
            .body(RequestResponseJsonObjects.makeExpectedCreateRequest()
            )
            .willRespondWith()
            .status(200)
            .body(RequestResponseJsonObjects.makeCreateResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment createWithId(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("Create with Id request")
            .path("/entity/default")
            .method("POST")
            .matchHeader("Content-Type", "application/json")
            .body(RequestResponseJsonObjects.makeExpectedCreateWithIdRequest())
            .willRespondWith()
            .status(200)
            .body(RequestResponseJsonObjects.makeCreateWithIdResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment createWithDuplicateId(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("Create with DuplicateId request")
            .path("/entity/default")
            .method("POST")
            .matchHeader("Content-Type", "application/json")
            .body(RequestResponseJsonObjects.makeExpectedCreateWithIdRequest()
            )
            .willRespondWith()
            .status(409)
            .body(RequestResponseJsonObjects.makeEntityExistsErrorResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment update(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("Update request")
            .path("/entity/default/" + RequestResponseJsonObjects.aggregateType + "/" + RequestResponseJsonObjects.ENTITY_ID)
            .method("POST")
            .matchHeader("Content-Type", "application/json")
            .body(RequestResponseJsonObjects.makeExpectedUpdateRequest()
            )
            .willRespondWith()
            .status(200)
            .body(RequestResponseJsonObjects.makeCreateResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment updateWithTriggeringEvent(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("updateWithTriggeringEvent request")
            .path("/entity/default/" + RequestResponseJsonObjects.aggregateType + "/" + RequestResponseJsonObjects.ENTITY_ID)
            .method("POST")
            .matchHeader("Content-Type", "application/json")
            .body(RequestResponseJsonObjects.makeExpectedUpdateRequestWithTriggeringEvent()
            )
            .willRespondWith()
            .status(200)
            .body(RequestResponseJsonObjects.makeCreateResponse())
            .toFragment();
  }
  @Pact(consumer="test_consumer")
  public PactFragment updateWithTriggeringEventConflict(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("updateWithTriggeringEventConflict request")
            .path("/entity/default/" + RequestResponseJsonObjects.aggregateType + "/" + RequestResponseJsonObjects.ENTITY_ID)
            .method("POST")
            .matchHeader("Content-Type", "application/json")
            .body(RequestResponseJsonObjects.makeExpectedUpdateRequestWithTriggeringEvent()
            )
            .willRespondWith()
            .status(409)
            .body(RequestResponseJsonObjects.makeDuplicateEventErrorResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment updateWithOptimisticLockingException(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("Update with Optimistic Locking request")
            .path("/entity/default/" + RequestResponseJsonObjects.aggregateType + "/" + RequestResponseJsonObjects.ENTITY_ID)
            .method("POST")
            .matchHeader("Content-Type", "application/json")
            .body(RequestResponseJsonObjects.makeExpectedUpdateRequest()
            )
            .willRespondWith()
            .status(409)
            .body(RequestResponseJsonObjects.makeOptimisticLockingErrorResponse())
            .toFragment();
  }
  @Pact(consumer="test_consumer")
  public PactFragment updateWithOptimisticLockingExceptionFollowingRetry(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("Update with Optimistic Locking request 2")
            .path("/entity/default/" + RequestResponseJsonObjects.aggregateType + "/" + RequestResponseJsonObjects.ENTITY_ID)
            .method("POST")
            .matchHeader("Content-Type", "application/json")
            .body(RequestResponseJsonObjects.makeExpectedUpdateRequest())
            .willRespondWith()
            .status(409)
            .body(RequestResponseJsonObjects.makeOptimisticLockingErrorResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment find(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("Find request")
            .path("/entity/default/" + RequestResponseJsonObjects.aggregateType + "/" + RequestResponseJsonObjects.ENTITY_ID)
            .method("GET")
            .willRespondWith()
            .status(200)
            .body(RequestResponseJsonObjects.makeFindResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment findWithTriggeringEvent(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("findWithTriggeringEvent request")
            .path("/entity/default/" + RequestResponseJsonObjects.aggregateType + "/" + RequestResponseJsonObjects.ENTITY_ID)
            .query("triggeringEventToken=myeventtoken")
            .method("GET")
            .willRespondWith()
            .status(200)
            .body(RequestResponseJsonObjects.makeFindResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment findWithTriggeringEventConflict(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("findWithTriggeringEventConflict request")
            .path("/entity/default/" + RequestResponseJsonObjects.aggregateType + "/" + RequestResponseJsonObjects.ENTITY_ID)
            .query("triggeringEventToken=myeventtoken")
            .method("GET")
            .willRespondWith()
            .status(409)
            .body(RequestResponseJsonObjects.makeDuplicateEventErrorResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment getNotFound(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("Find non existent request")
            .path("/entity/default/" + RequestResponseJsonObjects.aggregateType + "/" + RequestResponseJsonObjects.ENTITY_ID)
            .method("GET")
            .willRespondWith()
            .status(200)
            .body(RequestResponseJsonObjects.makeFindNonExistentResponse())
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment post500(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("POST Request causing 500")
            .matchPath("/entity/default.*")
            .method("POST")
            .willRespondWith()
            .status(500)
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment create500(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("Create Request causing 500")
            .matchPath("/entity/default.*")
            .method("POST")
            .willRespondWith()
            .status(500)
            .toFragment();
  }

  @Pact(consumer="test_consumer")
  public PactFragment get500(PactDslWithProvider builder) {
    return builder
            .given("test state")
            .uponReceiving("GET Request causing 500")
            .matchPath("/entity/default.*")
            .method("GET")
            .willRespondWith()
            .status(500)
            .toFragment();
  }

  private CompletableFuture<EntityIdVersionAndEventIds> save() {
    return client.save(RequestResponseJsonObjects.aggregateType,
            Collections.singletonList(new EventTypeAndData(RequestResponseJsonObjects.createdEvent, RequestResponseJsonObjects.eventData, Optional.empty())), Optional.empty());
  }

  @Test
  @PactVerification(fragment="create")
  public void shouldCreate() throws URISyntaxException, ExecutionException, InterruptedException {

  EntityIdVersionAndEventIds saveResult = save().get();

    assertEquals(RequestResponseJsonObjects.ENTITY_ID, saveResult.getEntityId());
    assertEquals(new Int128(1,2), saveResult.getEntityVersion());

    //assertEquals(Collections.singletonList(new Int128(3,4)), saveResult.getEventIds());

  }

  @Test
  @PactVerification(fragment="update")
  public void shouldUpdate() throws URISyntaxException, ExecutionException, InterruptedException {

    EntityIdVersionAndEventIds updateResult = update().get();

    assertEquals(RequestResponseJsonObjects.ENTITY_ID, updateResult.getEntityId());
    assertEquals(new Int128(1,2), updateResult.getEntityVersion());

    //assertEquals(Collections.singletonList(new Int128(3,4)), updateResult.getEventIds());

  }

  private CompletableFuture<EntityIdVersionAndEventIds> update() {
    return client.update(new EntityIdAndType(RequestResponseJsonObjects.ENTITY_ID, RequestResponseJsonObjects.aggregateType),
            new Int128(5, 6),
            Collections.singletonList(new EventTypeAndData(RequestResponseJsonObjects.debitedEvent, RequestResponseJsonObjects.eventData, Optional.empty())), Optional.empty());
  }

  @Test
  @PactVerification(fragment="find")
  public void shouldFind() throws URISyntaxException, ExecutionException, InterruptedException {
    LoadedEvents findResult = find().get();
    assertEquals(Collections.singletonList(new EventIdTypeAndData(new Int128(8,9),
            RequestResponseJsonObjects.createdEvent,
            RequestResponseJsonObjects.eventData,
            null)), findResult.getEvents());
  }

  private CompletableFuture<LoadedEvents> find() {
    return client.find(RequestResponseJsonObjects.aggregateType, RequestResponseJsonObjects.ENTITY_ID, Optional.empty());
  }


  @Test(expected=EventuateServerException.class)
  @PactVerification(fragment="create500")
  public void shouldCreate500() throws Throwable {
    decodeExecutionException(save());
  }

  @Test(expected=EventuateServerException.class)
  @PactVerification(fragment="post500")
  public void shouldUpdate500() throws Throwable {
    decodeExecutionException(update());
  }

  @Test(expected=EventuateServerException.class)
  @PactVerification(fragment="get500")
  public void shouldFind500() throws Throwable {
    decodeExecutionException(find());
  }


  private <T> T decodeExecutionException(CompletableFuture<T> future) throws Throwable {
    try {
      return future.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }



  @Test
  @PactVerification(fragment="createWithId")
  public void shouldCreateWithId() throws URISyntaxException, ExecutionException, InterruptedException {

    EntityIdVersionAndEventIds saveResult = saveWithId().get();

    assertEquals(RequestResponseJsonObjects.createId, saveResult.getEntityId());
    assertEquals(new Int128(1,2), saveResult.getEntityVersion());

    //assertEquals(Collections.singletonList(new Int128(3,4)), saveResult.getEventIds());

  }

  private CompletableFuture<EntityIdVersionAndEventIds> saveWithId() {
    return client.save(RequestResponseJsonObjects.aggregateType,
            Collections.singletonList(new EventTypeAndData(RequestResponseJsonObjects.createdEvent, RequestResponseJsonObjects.eventData, Optional.empty())),
            Optional.of(new AggregateCrudSaveOptions().withId(RequestResponseJsonObjects.createId)));
  }

  @Test(expected=EntityAlreadyExistsException.class)
  @PactVerification(fragment="createWithDuplicateId")
  public void shouldCreateWithDuplicateId() throws Throwable {
    decodeExecutionException(saveWithId());
  }

  @Test(expected= OptimisticLockingException.class)
  @PactVerification(fragment="updateWithOptimisticLockingException")
  public void shouldUpdateWithOptimisticLockingException() throws Throwable {
    decodeExecutionException(update());
  }

  @Test(expected= OptimisticLockingException.class)
  @PactVerification(fragment="updateWithOptimisticLockingExceptionFollowingRetry")
  public void shouldUpdateWithOptimisticLockingExceptionFollowingRetry() throws Throwable {
    decodeExecutionException(update());
  }

  @Test(expected=EntityNotFoundException.class)
  @PactVerification(fragment="getNotFound")
  public void shouldFindNonExistent() throws Throwable {
    decodeExecutionException(find());
  }

   @Test
  @PactVerification(fragment="findWithTriggeringEvent")
  public void shouldFindWithTriggeringEvent() throws URISyntaxException, ExecutionException, InterruptedException {
    LoadedEvents findResult = findWithEventContext().get();
    assertEquals(Collections.singletonList(new EventIdTypeAndData(new Int128(8,9), RequestResponseJsonObjects.createdEvent, RequestResponseJsonObjects.eventData, null)), findResult.getEvents());
  }

  private CompletableFuture<LoadedEvents> findWithEventContext() {
    return client.find(RequestResponseJsonObjects.aggregateType, RequestResponseJsonObjects.ENTITY_ID, Optional.of(new AggregateCrudFindOptions().withTriggeringEvent(Optional.of(RequestResponseJsonObjects.makeEventContext()))));
  }

  @Test(expected=DuplicateTriggeringEventException.class)
  @PactVerification(fragment="findWithTriggeringEventConflict")
  public void shouldFindWithTriggeringEventConflict() throws Throwable {
    decodeExecutionException(findWithEventContext());
  }

  @Test
  @PactVerification(fragment="updateWithTriggeringEvent")
  public void shouldUpdateWithTriggeringEvent() throws URISyntaxException, ExecutionException, InterruptedException {

    EntityIdVersionAndEventIds updateResult = updateWithEventContext().get();

    assertEquals(RequestResponseJsonObjects.ENTITY_ID, updateResult.getEntityId());
    assertEquals(new Int128(1,2), updateResult.getEntityVersion());

    //assertEquals(Collections.singletonList(new Int128(3,4)), updateResult.getEventIds());

  }

  private CompletableFuture<EntityIdVersionAndEventIds> updateWithEventContext() {
    return client.update(new EntityIdAndType(RequestResponseJsonObjects.ENTITY_ID, RequestResponseJsonObjects.aggregateType),
            new Int128(5, 6),
            Collections.singletonList(new EventTypeAndData(RequestResponseJsonObjects.debitedEvent, RequestResponseJsonObjects.eventData, Optional.empty())),
            Optional.of(new AggregateCrudUpdateOptions().withTriggeringEvent(RequestResponseJsonObjects.makeEventContext())));
  }

  @Test(expected=DuplicateTriggeringEventException.class)
  @PactVerification(fragment="updateWithTriggeringEventConflict")
  public void shouldUpdateWithTriggeringEventConflict() throws Throwable {

    decodeExecutionException(updateWithEventContext());

  }

  // TODO update non existent



}