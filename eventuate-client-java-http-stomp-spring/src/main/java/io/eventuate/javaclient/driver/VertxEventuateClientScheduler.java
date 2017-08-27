package io.eventuate.javaclient.driver;

import io.eventuate.javaclient.eventhandling.exceptionhandling.EventuateClientScheduler;
import io.vertx.core.Vertx;

public class VertxEventuateClientScheduler implements EventuateClientScheduler {
  private Vertx vertx;

  public VertxEventuateClientScheduler(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void setTimer(long delayInMilliseconds, Runnable callback) {
    vertx.setTimer(delayInMilliseconds, i -> callback.run());
  }
}
