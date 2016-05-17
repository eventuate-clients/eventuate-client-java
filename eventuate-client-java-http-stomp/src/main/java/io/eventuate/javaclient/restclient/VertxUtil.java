package io.eventuate.javaclient.restclient;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class VertxUtil {

  public static Context getContext(Vertx vertx) {
    Context context = vertx.getOrCreateContext();

    if (context.isEventLoopContext()) {
      System.out.println("Context attached to Event Loop");
    } else if (context.isWorkerContext()) {
      System.out.println("Context attached to Worker Thread");
    } else if (context.isMultiThreadedWorkerContext()) {
      System.out.println("Context attached to Worker Thread - multi threaded worker");
    } else if (! Context.isOnVertxThread()) {
      System.out.println("Context not attached to a thread managed by vert.x");
    }
    return context;
  }
}
