package io.eventuate.javaclient.spring.jdbc;

import io.eventuate.Int128;
import org.junit.Test;

import static org.junit.Assert.*;

public class IdGeneratorImplTest {

  @Test
  public void shouldGenerateId() {
    IdGeneratorImpl idGen = new IdGeneratorImpl();
    Int128 id = idGen.genId();
    assertNotNull(id);
  }

  @Test
  public void shouldGenerateMonotonicId() {
    IdGeneratorImpl idGen = new IdGeneratorImpl();
    Int128 id1 = idGen.genId();
    Int128 id2 = idGen.genId();
    assertTrue(id1.compareTo(id2) < 0);
  }

}