package io.eventuate.javaclient.spring.autoconfiguration;

import io.eventuate.Aggregate;
import io.eventuate.EntityIdAndVersion;
import io.eventuate.EntityWithMetadata;
import io.eventuate.Event;
import io.eventuate.EventuateAggregateStore;
import io.eventuate.SaveOptions;
import io.eventuate.javaclient.spring.tests.common.AbstractAccountIntegrationReactiveTest;
import io.eventuate.javaclient.saasclient.EventuateAggregateStoreBuilder;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;

import static io.eventuate.testutil.AsyncUtil.await;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AutoConfigurationIntegrationTestConfiguration.class)
@IntegrationTest
public class EventuateAggregateStoreBuilderIntegrationTest extends AbstractAccountIntegrationReactiveTest {

  private EventuateAggregateStore aggregateStore = EventuateAggregateStoreBuilder.defaultFromEnv();

  @Override
  protected <T extends Aggregate<T>> EntityIdAndVersion save(Class<T> classz, List<Event> events, Optional<SaveOptions> saveOptions) {
    return await(aggregateStore.save(classz, events, saveOptions));
  }

  @Override
  protected <T extends Aggregate<T>> EntityWithMetadata<T> find(Class<T> clasz, String entityId) {
    return await(aggregateStore.find(clasz, entityId));
  }
}
