package io.eventuate.javaclient.domain;

import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class SwimlaneDispatcher {
  
  private static Logger logger = LoggerFactory.getLogger(SwimlaneDispatcher.class);

  private String subscriberId;
  private Integer swimlane;
  private Executor executor;

  private final LinkedBlockingQueue<QueuedEvent> queue = new LinkedBlockingQueue<>();
  private AtomicBoolean running = new AtomicBoolean(false);

  public SwimlaneDispatcher(String subscriberId, Integer swimlane, Executor executor) {
    this.subscriberId = subscriberId;
    this.swimlane = swimlane;
    this.executor = executor;
  }

  public CompletableFuture<?> dispatch(DispatchedEvent<Event> de, Function<DispatchedEvent<Event>, CompletableFuture<?>> target) {
    synchronized (queue) {
      QueuedEvent qe = new QueuedEvent(de, target);
      queue.add(qe);
      logger.trace("added event to queue: {}",  swimlane);
      if (running.compareAndSet(false, true)) {
        logger.trace("Started thread: {}", swimlane);
        executor.execute(new MyRunnable(swimlane));
      } else
        logger.trace("Not started thread: {}", swimlane);
      return qe.future;
    }
  }

  class QueuedEvent {
    DispatchedEvent<Event> event;
    private Function<DispatchedEvent<Event>, CompletableFuture<?>> target;
    CompletableFuture<Object> future = new CompletableFuture<>();

    public QueuedEvent(DispatchedEvent<Event> event, Function<DispatchedEvent<Event>, CompletableFuture<?>> target) {
      this.event = event;
      this.target = target;
    }
  }


  private class MyRunnable implements Runnable {
    private Integer swimlane;

    public MyRunnable(Integer swimlane) {
      this.swimlane = swimlane;
    }

    @Override
    public void run() {
      logger.trace("Starting thread for {}", swimlane);
      while (true) {
        QueuedEvent qe = getNextEvent();
        if (qe == null)
          break;
        logger.trace("Processing event for {}", swimlane);
        qe.target.apply(qe.event).handle((success, throwable) -> {
          if (throwable == null) {
            logger.info("Handler succeeded");
            boolean x = qe.future.complete(success);
            logger.trace("Completed future success {}", x);
          } else {
            logger.error("handler for " + subscriberId + " for event " + qe.event + " failed: ", throwable);
            boolean x = qe.future.completeExceptionally(throwable);
            logger.trace("Completed future failed{}", x);
          }
          return null;
        });
      }
      logger.trace("Exiting thread for {}", swimlane);
    }

    private QueuedEvent getNextEvent() {
      QueuedEvent qe1 = queue.poll();
      if (qe1 != null)
        return qe1;

      synchronized (queue) {
        QueuedEvent qe = queue.poll();
        if (qe == null) {
          running.compareAndSet(true, false);
        }
        return qe;
      }
    }
  }
}
