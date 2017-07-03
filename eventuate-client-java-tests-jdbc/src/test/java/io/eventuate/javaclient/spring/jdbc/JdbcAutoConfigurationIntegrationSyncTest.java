package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.AggregateRepository;
import io.eventuate.javaclient.spring.tests.common.AbstractAccountIntegrationSyncTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = JdbcAutoConfigurationIntegrationTestConfiguration.class)
@IntegrationTest
public class JdbcAutoConfigurationIntegrationSyncTest extends AbstractAccountIntegrationSyncTest {


}
