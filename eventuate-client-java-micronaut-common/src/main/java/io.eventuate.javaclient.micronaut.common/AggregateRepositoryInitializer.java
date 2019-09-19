package io.eventuate.javaclient.micronaut.common;

import io.eventuate.AggregateRepository;
import io.eventuate.CompositeMissingApplyEventMethodStrategy;
import io.micronaut.context.annotation.Context;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Context
public class AggregateRepositoryInitializer {

  @Inject
  private CompositeMissingApplyEventMethodStrategy strategies;

  @Inject
  private AggregateRepository[] aggregateRepositories;

  @Inject
  private io.eventuate.sync.AggregateRepository[] syncAggregateRepositories;

  @PostConstruct
  public void setMissingStrategies() {
    for (AggregateRepository aggregateRepository : aggregateRepositories) {
      aggregateRepository.setMissingApplyEventMethodStrategy(strategies);
    }

    for (io.eventuate.sync.AggregateRepository aggregateRepository : syncAggregateRepositories) {
      aggregateRepository.setMissingApplyEventMethodStrategy(strategies);

    }
  }
}
