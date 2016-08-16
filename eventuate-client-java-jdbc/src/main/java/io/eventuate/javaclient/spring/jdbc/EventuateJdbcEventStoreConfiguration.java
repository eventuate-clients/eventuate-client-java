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

import javax.sql.DataSource;

/**
 * Defines the Spring beans for the embedded, JDBC-based event store
 */
@Configuration
public class EventuateJdbcEventStoreConfiguration {

  @Bean
  public EventuateJdbcEventStore eventuateJdbcEventStore() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
    return new EventuateJdbcEventStore(jdbcTemplate);
  }

  @Bean
  public DataSource dataSource() {
    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    return builder.setType(EmbeddedDatabaseType.H2).addScript("embedded-event-store-schema.sql").build();
  }

  @Bean
  public EventuateAggregateStore httpStompEventStore(AggregateCrud restClient, AggregateEvents stompClient) {
    return new EventuateAggregateStoreImpl(restClient, stompClient);
  }

}
