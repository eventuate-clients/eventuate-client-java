package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.*;
import io.eventuate.javaclient.commonimpl.AggregateCrud;
import io.eventuate.javaclient.commonimpl.EntityIdVersionAndEventIds;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.eventuate.testutil.AsyncUtil.await;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = EventuateJdbcEventStoreTest.EventuateJdbcEventStoreTestConfiguration.class)
@IntegrationTest
public class EventuateJdbcEventStoreTest {

  private final EventContext ectx = new EventContext("MyEventToken");
  private final String aggregateType = "MyAggregateType";

  @Autowired
  private AggregateCrud eventStore;

  @Test
  public void findShouldCompleteWithDuplicateTriggeringEventException() throws ExecutionException, InterruptedException {
    EntityIdVersionAndEventIds eidv = await(eventStore.save(aggregateType,
            Collections.singletonList(new EventTypeAndData("MyEventType", "{}")),
            Optional.of(new SaveOptions().withEventContext(ectx))));
    CompletableFuture<LoadedEvents> c = eventStore.find(aggregateType, eidv.getEntityId(), Optional.of(new FindOptions().withTriggeringEvent(ectx)));
    shouldCompletedExceptionally(c, DuplicateTriggeringEventException.class);
  }

  @Test
  public void updateShouldCompleteWithOptimisticLockingException() throws ExecutionException, InterruptedException {
    EntityIdVersionAndEventIds eidv = await(eventStore.save(aggregateType,
            Collections.singletonList(new EventTypeAndData("MyEventType", "{}")),
            Optional.of(new SaveOptions().withEventContext(ectx))));
    CompletableFuture<EntityIdVersionAndEventIds> c = eventStore.update(new EntityIdAndType(eidv.getEntityId(), aggregateType),
            new Int128(0,0), Collections.singletonList(new EventTypeAndData("MyEventType", "{}")), Optional.of(new UpdateOptions()));
    shouldCompletedExceptionally(c, OptimisticLockingException.class);
  }


  private void shouldCompletedExceptionally(CompletableFuture<?> c, Class<?> exceptionClass) throws InterruptedException, ExecutionException {
    try {
      c.get();
      fail();
    } catch (ExecutionException e) {
      if (!exceptionClass.isInstance(e.getCause()))
        throw e;
    }
  }

  @Configuration
  @Import(EventuateJdbcEventStoreConfiguration.class)
  @EnableAutoConfiguration
  public static class EventuateJdbcEventStoreTestConfiguration {

  }
}