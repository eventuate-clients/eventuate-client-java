package io.eventuate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class CompletableFutureUtil {
  public static Throwable unwrap(Throwable throwable) {
    return throwable instanceof CompletionException ? throwable.getCause() : throwable;
  }

  public static <T> CompletableFuture<T> failedFuture(Throwable t) {
    CompletableFuture<T> f = new CompletableFuture<>();
    f.completeExceptionally(t);
    return f;
  }
}
