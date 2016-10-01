package io.eventuate.javaclient.spring.httpstomp;

import io.eventuate.javaclient.driver.EventuateDriverConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Defines the Spring beans to connect to an Eventuate server
 *
 * @deprecated  use {@link io.eventuate.javaclient.driver.EventuateDriverConfiguration} instead
 * @see io.eventuate.javaclient.driver.EventuateDriverConfiguration
 */
@Configuration
@Import(EventuateDriverConfiguration.class)
public class EventuateHttpStompClientConfiguration {

}
