package io.eventuate.javaclient.jdbc.micronaut;

import io.eventuate.javaclient.jdbc.EventuateJdbcAccess;
import io.eventuate.javaclient.jdbc.common.tests.CommonEventuateJdbcAccessImplTest;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;

@MicronautTest(transactional = false)
public class EventuateJdbcAccessImplTest extends CommonEventuateJdbcAccessImplTest {
  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private JdbcTemplate jdbcTemplate;

  @Inject
  private EventuateJdbcAccess eventuateJdbcAccess;

  @Override
  protected String readAllEventsSql() {
    return "select * from eventuate.events";
  }

  @Override
  protected String readAllEntitiesSql() {
    return "select * from eventuate.entities";
  }

  @Override
  protected String readAllSnapshots() {
    return "select * from eventuate.snapshots";
  }

  @BeforeEach
  public void init() {
    applicationContext.publishEvent(new RefreshEvent());
  }

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
