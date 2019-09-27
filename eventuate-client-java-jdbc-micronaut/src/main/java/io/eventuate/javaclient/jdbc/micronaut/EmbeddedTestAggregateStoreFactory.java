package io.eventuate.javaclient.jdbc.micronaut;

import io.eventuate.common.jdbc.EventuateCommonJdbcOperations;
import io.eventuate.javaclient.commonimpl.AggregateCrud;
import io.eventuate.javaclient.commonimpl.AggregateEvents;
import io.eventuate.javaclient.commonimpl.adapters.SyncToAsyncAggregateCrudAdapter;
import io.eventuate.javaclient.commonimpl.adapters.SyncToAsyncAggregateEventsAdapter;
import io.eventuate.javaclient.eventhandling.exceptionhandling.EventuateClientScheduler;
import io.eventuate.javaclient.jdbc.EventuateEmbeddedTestAggregateStore;
import io.eventuate.javaclient.jdbc.EventuateJdbcAccess;
import io.eventuate.javaclient.jdbc.EventuateJdbcAccessImpl;
import io.eventuate.javaclient.jdbc.JdkTimerBasedEventuateClientScheduler;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Singleton;
import javax.sql.DataSource;

@Factory
public class EmbeddedTestAggregateStoreFactory {

  @Singleton
  @Primary
  public EventuateJdbcAccess eventuateJdbcAccess(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate, EventuateCommonJdbcOperations eventuateCommonJdbcOperations) {
    return new EventuateJdbcAccessImpl(transactionTemplate, jdbcTemplate, eventuateCommonJdbcOperations);
  }

  @Singleton
  @Primary
  public EventuateEmbeddedTestAggregateStore eventuateEmbeddedTestAggregateStore(EventuateJdbcAccess eventuateJdbcAccess) {
    return new EventuateEmbeddedTestAggregateStore(eventuateJdbcAccess);
  }

  @Singleton
  public DataSource dataSource() {
    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    return builder.setType(EmbeddedDatabaseType.H2).addScript("eventuate-embedded-schema.sql").build();
  }

  @Singleton
  @Primary
  public AggregateCrud aggregateCrud(io.eventuate.javaclient.commonimpl.sync.AggregateCrud aggregateCrud) {
    return new SyncToAsyncAggregateCrudAdapter(aggregateCrud);
  }

  @Singleton
  public AggregateEvents aggregateEvents(io.eventuate.javaclient.commonimpl.sync.AggregateEvents aggregateEvents) {
    return new SyncToAsyncAggregateEventsAdapter(aggregateEvents);
  }

  @Singleton
  public EventuateClientScheduler eventHandlerRecoveryScheduler() {
    return new JdkTimerBasedEventuateClientScheduler();
  }

  @Singleton
  @Requires(missingBeans = TransactionTemplate.class)
  public TransactionTemplate transactionTemplate(DataSource dataSource) {
    return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }
}
