package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.DuplicateTriggeringEventException;
import io.eventuate.EntityIdAndType;
import io.eventuate.EventContext;
import io.eventuate.Int128;
import io.eventuate.OptimisticLockingException;
import io.eventuate.javaclient.commonimpl.AggregateCrudFindOptions;
import io.eventuate.javaclient.commonimpl.AggregateCrudSaveOptions;
import io.eventuate.javaclient.commonimpl.AggregateCrudUpdateOptions;
import io.eventuate.javaclient.commonimpl.EntityIdVersionAndEventIds;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;
import io.eventuate.javaclient.commonimpl.SerializedSnapshot;
import io.eventuate.javaclient.commonimpl.sync.AggregateCrud;
import io.eventuate.javaclient.spring.EventuateJavaClientDomainConfiguration;
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
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = EventuateEmbeddedTestAggregateStoreTest.EventuateJdbcEventStoreTestConfiguration.class)
@IntegrationTest
public class EventuateEmbeddedTestAggregateStoreTest {

  private final EventContext ectx = new EventContext("MyEventToken");
  private final String aggregateType = "MyAggregateType";

  @Autowired
  private AggregateCrud eventStore;

  @Test
  public void findShouldCompleteWithDuplicateTriggeringEventException() throws ExecutionException, InterruptedException {
    EntityIdVersionAndEventIds eidv = eventStore.save(aggregateType,
            Collections.singletonList(new EventTypeAndData("MyEventType", "{}")),
            Optional.of(new AggregateCrudSaveOptions().withEventContext(ectx)));
    shouldCompletedExceptionally(DuplicateTriggeringEventException.class, () -> eventStore.find(aggregateType, eidv.getEntityId(), Optional.of(new AggregateCrudFindOptions().withTriggeringEvent(ectx))));
  }

  @Test
  public void updateShouldCompleteWithOptimisticLockingException() throws ExecutionException, InterruptedException {
    EntityIdVersionAndEventIds eidv = eventStore.save(aggregateType,
            Collections.singletonList(new EventTypeAndData("MyEventType", "{}")),
            Optional.of(new AggregateCrudSaveOptions().withEventContext(ectx)));
    shouldCompletedExceptionally(OptimisticLockingException.class, () -> eventStore.update(new EntityIdAndType(eidv.getEntityId(), aggregateType),
            new Int128(0,0), Collections.singletonList(new EventTypeAndData("MyEventType", "{}")),
            Optional.of(new AggregateCrudUpdateOptions())));
  }


  private <T> void shouldCompletedExceptionally(Class<? extends Throwable> exceptionClass, Supplier<T> body)  {
    try {
      body.get();
      fail();
    } catch (Throwable e) {
      if (!exceptionClass.isInstance(e))
        throw e;
    }
  }

  @Configuration
  @Import({EmbeddedTestAggregateStoreConfiguration.class, EventuateJavaClientDomainConfiguration.class})
  @EnableAutoConfiguration
  public static class EventuateJdbcEventStoreTestConfiguration {

  }

  @Test
  public void shouldSaveAndLoadSnapshot() {

    EntityIdVersionAndEventIds eidv = eventStore.save(aggregateType,
            Collections.singletonList(new EventTypeAndData("MyEventType", "{}")),
            Optional.of(new AggregateCrudSaveOptions().withEventContext(ectx)));

    EntityIdVersionAndEventIds updateResult = eventStore.update(
            new EntityIdAndType(eidv.getEntityId(), aggregateType),
            eidv.getEntityVersion(),
            Collections.singletonList(new EventTypeAndData("MyEventType", "{}")),
            Optional.of(new AggregateCrudUpdateOptions().withSnapshot(new SerializedSnapshot("X", "Y"))));

    LoadedEvents findResult = eventStore.find(aggregateType, eidv.getEntityId(), Optional.of(new AggregateCrudFindOptions()));

    assertTrue(findResult.getSnapshot().isPresent());
    assertTrue(findResult.getEvents().isEmpty());

  }
}