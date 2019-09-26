package io.eventuate.javaclient.jdbc.micronaut;

import io.eventuate.common.jdbc.EventuateSchema;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.runtime.context.scope.Refreshable;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Singleton;
import javax.sql.DataSource;

@Factory
public class EventuateJdbcAccessImplTestFactory {
  @Singleton
  public PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Refreshable
  @Primary
  public DataSource dataSource() {
    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    return builder.setType(EmbeddedDatabaseType.H2).addScript("eventuate-embedded-schema.sql").build();
  }

  @Singleton
  @Primary
  public EventuateSchema eventuateSchema() {
    return new EventuateSchema();
  }
}
