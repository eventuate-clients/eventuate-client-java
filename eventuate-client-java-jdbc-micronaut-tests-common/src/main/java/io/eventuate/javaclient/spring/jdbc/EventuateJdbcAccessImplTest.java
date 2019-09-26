package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.EntityIdAndType;
import io.eventuate.common.jdbc.EventuateCommonJdbcOperations;
import io.eventuate.common.jdbc.EventuateSchema;
import io.eventuate.javaclient.commonimpl.AggregateCrudUpdateOptions;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;
import io.eventuate.javaclient.commonimpl.SerializedSnapshot;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public abstract class EventuateJdbcAccessImplTest {

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

  private static final String testAggregate = "testAggregate1";
  private static final String testEventType = "testEventType1";
  private static final String testEventData = "testEventData1";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private EventuateJdbcAccess eventuateJdbcAccess;

  protected abstract String readAllEventsSql();

  protected abstract String readAllEntitiesSql();

  protected abstract String readAllSnapshots();

  @Test
  public void testSave() {
    EventTypeAndData eventTypeAndData = new EventTypeAndData(testEventType, testEventData, Optional.empty());

    eventuateJdbcAccess.save(testAggregate, Collections.singletonList(eventTypeAndData), Optional.empty());

    List<Map<String, Object>> events = jdbcTemplate.queryForList(readAllEventsSql());
    Assert.assertEquals(1, events.size());

    List<Map<String, Object>> entities = jdbcTemplate.queryForList(readAllEntitiesSql());
    Assert.assertEquals(1, entities.size());
  }

  @Test
  public void testFind() {
    EventTypeAndData eventTypeAndData = new EventTypeAndData(testEventType, testEventData, Optional.empty());

    SaveUpdateResult saveUpdateResult = eventuateJdbcAccess.save(testAggregate, Collections.singletonList(eventTypeAndData), Optional.empty());

    LoadedEvents loadedEvents = eventuateJdbcAccess.find(testAggregate, saveUpdateResult.getEntityIdVersionAndEventIds().getEntityId(), Optional.empty());

    Assert.assertEquals(1, loadedEvents.getEvents().size());
  }

  @Test
  public void testUpdate() {
    EventTypeAndData eventTypeAndData = new EventTypeAndData(testEventType, testEventData, Optional.empty());
    SaveUpdateResult saveUpdateResult = eventuateJdbcAccess.save(testAggregate, Collections.singletonList(eventTypeAndData), Optional.empty());


    EntityIdAndType entityIdAndType = new EntityIdAndType(saveUpdateResult.getEntityIdVersionAndEventIds().getEntityId(), testAggregate);
    eventTypeAndData = new EventTypeAndData("testEventType2", "testEventData2", Optional.empty());

    eventuateJdbcAccess.update(entityIdAndType,
            saveUpdateResult.getEntityIdVersionAndEventIds().getEntityVersion(),
            Collections.singletonList(eventTypeAndData), Optional.of(new AggregateCrudUpdateOptions(Optional.empty(), Optional.of(new SerializedSnapshot("", "")))));

    List<Map<String, Object>> events = jdbcTemplate.queryForList(readAllEventsSql());
    Assert.assertEquals(2, events.size());

    List<Map<String, Object>> entities = jdbcTemplate.queryForList(readAllEntitiesSql());
    Assert.assertEquals(1, entities.size());

    List<Map<String, Object>> snapshots = jdbcTemplate.queryForList(readAllSnapshots());
    Assert.assertEquals(1, snapshots.size());

    LoadedEvents loadedEvents = eventuateJdbcAccess.find(testAggregate, saveUpdateResult.getEntityIdVersionAndEventIds().getEntityId(), Optional.empty());
    Assert.assertTrue(loadedEvents.getSnapshot().isPresent());
  }

  protected List<String> loadSqlScriptAsListOfLines(String script) throws IOException {
    try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/eventuate-embedded-schema.sql")))) {
      return bufferedReader.lines().collect(Collectors.toList());
    }
  }

  protected void executeSql(List<String> sqlList) {
    jdbcTemplate.execute(sqlList.stream().collect(Collectors.joining("\n")));
  }
}
