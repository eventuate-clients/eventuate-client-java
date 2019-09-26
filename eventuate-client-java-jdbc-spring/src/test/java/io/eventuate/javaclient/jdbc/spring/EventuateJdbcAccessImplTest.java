package io.eventuate.javaclient.jdbc.spring;

import io.eventuate.common.jdbc.EventuateCommonJdbcOperations;
import io.eventuate.common.jdbc.EventuateSchema;
import io.eventuate.javaclient.jdbc.EventuateJdbcAccess;
import io.eventuate.javaclient.jdbc.EventuateJdbcAccessImpl;
import io.eventuate.javaclient.jdbc.common.tests.CommonEventuateJdbcAccessImplTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

public abstract class EventuateJdbcAccessImplTest extends CommonEventuateJdbcAccessImplTest {

  public static class Config {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    public EventuateCommonJdbcOperations eventuateCommonJdbcOperations(JdbcTemplate jdbcTemplate) {
      return new EventuateCommonJdbcOperations(jdbcTemplate);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EventuateJdbcAccess eventuateJdbcAccess(TransactionTemplate transactionTemplate,
                                                   JdbcTemplate jdbcTemplate,
                                                   EventuateCommonJdbcOperations eventuateCommonJdbcOperations,
                                                   EventuateSchema eventuateSchema) {
      return new EventuateJdbcAccessImpl(transactionTemplate, jdbcTemplate, eventuateCommonJdbcOperations, eventuateSchema);
    }

    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
      return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }
  }

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private EventuateJdbcAccess eventuateJdbcAccess;


  @Test
  @Override
  public void testSave() {
    super.testSave();
  }

  @Test
  @Override
  public void testFind() {
    super.testFind();
  }

  @Test
  @Override
  public void testUpdate() {
    super.testUpdate();
  }

  @Override
  protected JdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  @Override
  protected EventuateJdbcAccess getEventuateJdbcAccess() {
    return eventuateJdbcAccess;
  }
}
