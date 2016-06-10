package io.eventuate.javaclient.spring.autoconfiguration;

import io.eventuate.javaclient.spring.tests.common.AbstractAccountIntegrationTest;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AutoConfigurationIntegrationTestConfiguration.class)
@IntegrationTest
public class AutoConfigurationIntegrationTest extends AbstractAccountIntegrationTest {


}
