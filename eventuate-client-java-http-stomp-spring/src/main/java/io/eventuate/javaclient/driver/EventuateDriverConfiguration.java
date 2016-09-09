package io.eventuate.javaclient.driver;

import io.eventuate.javaclient.spring.httpstomp.EventuateHttpStompClientConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(EventuateHttpStompClientConfiguration.class)
public class EventuateDriverConfiguration {
}
