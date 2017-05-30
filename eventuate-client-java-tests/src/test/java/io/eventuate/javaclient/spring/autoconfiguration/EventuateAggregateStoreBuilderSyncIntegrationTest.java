package io.eventuate.javaclient.spring.autoconfiguration;

import io.eventuate.Aggregate;
import io.eventuate.EntityIdAndVersion;
import io.eventuate.EntityWithMetadata;
import io.eventuate.Event;
import io.eventuate.SaveOptions;
import io.eventuate.javaclient.saasclient.EventuateAggregateStoreBuilder;
import io.eventuate.javaclient.spring.tests.common.AbstractAccountIntegrationSyncTest;
import io.eventuate.sync.EventuateAggregateStore;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AutoConfigurationIntegrationTestConfiguration.class)
@IntegrationTest
public class EventuateAggregateStoreBuilderSyncIntegrationTest extends AbstractAccountIntegrationSyncTest {

  private EventuateAggregateStore aggregateStore = EventuateAggregateStoreBuilder.standard().buildSync();

  @Override
  protected <T extends Aggregate<T>> EntityIdAndVersion save(Class<T> classz, List<Event> events, Optional<SaveOptions> saveOptions) {
    return aggregateStore.save(classz, events, saveOptions);
  }

  @Override
  protected <T extends Aggregate<T>> EntityWithMetadata<T> find(Class<T> clasz, String entityId) {
    return aggregateStore.find(clasz, entityId);
  }
}
