package io.eventuate.javaclient.spring.autoconfiguration;

import io.eventuate.example.banking.services.JavaIntegrationTestDomainConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import(JavaIntegrationTestDomainConfiguration.class)
public class AutoConfigurationIntegrationTestConfiguration {
}
