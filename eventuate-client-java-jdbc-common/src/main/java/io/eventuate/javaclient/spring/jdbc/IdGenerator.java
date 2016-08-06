package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.Int128;

public interface IdGenerator {
  Int128 genId();
}
