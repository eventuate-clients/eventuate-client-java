package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.javaclient.commonimpl.EventuateAggregateStoreImpl;
import io.eventuate.javaclient.commonimpl.AggregateCrud;
import io.eventuate.javaclient.commonimpl.AggregateEvents;
import io.eventuate.EventuateAggregateStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Defines the Spring beans for the embedded, JDBC-based event store
 */
@Configuration
public class EventuateJdbcEventStoreConfiguration {

  @Bean
  public EventuateJdbcEventStore eventuateJdbcEventStore() {
    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    EmbeddedDatabase db = builder.setType(EmbeddedDatabaseType.H2).addScript("embedded-event-store-schema.sql").build();
    JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
    return new EventuateJdbcEventStore(jdbcTemplate);
  }

  @Bean
  public EventuateAggregateStore httpStompEventStore(AggregateCrud restClient, AggregateEvents stompClient) {
    return new EventuateAggregateStoreImpl(restClient, stompClient);
  }

}
