package io.eventuate.example.banking.services.counting;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.util.concurrent.atomic.AtomicLong;

@Aspect
public class InvocationCountingAspect {

  @Pointcut("@within(io.eventuate.example.banking.services.counting.Countable) && execution(public void *(..))")
  public void invocation() {}

  private AtomicLong counter = new AtomicLong(0);

  @Before("invocation()")
  public void countInvocation() {
    counter.incrementAndGet();
  }

  public long getCounter() {
    return counter.get();
  }
}
