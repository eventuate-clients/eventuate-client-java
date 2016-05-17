package io.eventuate.javaclient.restclient;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class VertxUtil {
    private static Logger logger = LoggerFactory.getLogger(VertxUtil.class);

  public static Context getContext(Vertx vertx) {
    Context context = vertx.getOrCreateContext();


    if (context.isEventLoopContext()) {
      logger.trace("Context attached to Event Loop");
    } else if (context.isWorkerContext()) {
      logger.trace("Context attached to Worker Thread");
    } else if (context.isMultiThreadedWorkerContext()) {
      logger.trace("Context attached to Worker Thread - multi threaded worker");
    } else if (! Context.isOnVertxThread()) {
      logger.trace("Context not attached to a thread managed by vert.x");
    }
    return context;
  }
}
