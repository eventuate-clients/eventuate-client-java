package io.eventuate.javaclient.domain;

import java.lang.reflect.Method;

public interface EventHandlerProcessor {
  boolean supports(Method method);

  EventHandler process(Object eventHandler, Method method);
}
