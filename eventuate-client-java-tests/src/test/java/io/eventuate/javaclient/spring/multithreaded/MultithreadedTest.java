package io.eventuate.javaclient.spring.multithreaded;


import io.eventuate.EntityIdAndVersion;
import io.eventuate.Event;
import io.eventuate.EventuateAggregateStore;
import io.eventuate.EventuateServiceUnavailableException;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.eventuate.example.banking.domain.Account;
import io.eventuate.example.banking.domain.CreateAccountCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MultithreadedTestConfiguration.class)
public class MultithreadedTest {

  @Autowired
  private EventuateAggregateStore aggregateStore;

  Executor pool = Executors.newCachedThreadPool();

  @Test
  public void shouldWorkWithMultipleThreads() throws InterruptedException {
    int n = 16;
    int loops = 10;
    LinkedBlockingQueue<Throwable> exceptions = new LinkedBlockingQueue<>();

    CyclicBarrier startBarrier = new CyclicBarrier(n);

    CountDownLatch endLatch = new CountDownLatch(n);

    for (int i = 0; i < n; i++)
      pool.execute(new MyRunnable(i, startBarrier, loops, endLatch, exceptions));

    endLatch.await(30, TimeUnit.SECONDS);

    if (!exceptions.isEmpty()) {
      for (Throwable t : exceptions.toArray(new Throwable[exceptions.size()])) {
        t.printStackTrace();
      }
      fail("Exceptions thrown");

    }
  }

  class MyRunnable implements Runnable {

    private int idx;
    private final CyclicBarrier startBarrier;
    private final int loops;
    private final CountDownLatch endLatch;
    private LinkedBlockingQueue<Throwable> exceptions;

    public MyRunnable(int idx, CyclicBarrier startBarrier, int loops, CountDownLatch endLatch, LinkedBlockingQueue<Throwable> exceptions) {
      this.idx = idx;
      this.startBarrier = startBarrier;
      this.loops = loops;
      this.endLatch = endLatch;
      this.exceptions = exceptions;
    }

    @Override
    public void run() {
      try {
        startBarrier.await();
        for (int i = 0; i < loops; i++) {
          Account account = new Account();
          List<Event> accountEvents = account.process(new CreateAccountCommand(new BigDecimal(12345)));

          EntityIdAndVersion accountEntity = withRetry(() -> aggregateStore.save(Account.class, accountEvents, Optional.empty())).get();

        }

        endLatch.countDown();
      } catch (InterruptedException | BrokenBarrierException | ExecutionException e) {
        try {
          exceptions.put(e);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
        endLatch.countDown();
      }
    }
  }

  private <T> Future<T> withRetry(Supplier<CompletableFuture<T>> asyncRequest) {
    CompletableFuture<T> result = new CompletableFuture<>();
    attemptOperation(asyncRequest, result);
    return result;
  }

  @Autowired
  private Vertx vertx;

  private <T> void attemptOperation(Supplier<CompletableFuture<T>> asyncRequest, CompletableFuture<T> result) {
    CompletableFuture<T> f = asyncRequest.get();
    f.handleAsync((val, throwable) -> {
      if (throwable != null) {
        if (throwable instanceof EventuateServiceUnavailableException) {
          vertx.setTimer(1000, new Handler<Long>() {
            @Override
            public void handle(Long event) {
              attemptOperation(asyncRequest, result);
            }
          });
        } else
        result.completeExceptionally(throwable);
      }
      else
        result.complete(val);
      return null;
    });
  }
}
