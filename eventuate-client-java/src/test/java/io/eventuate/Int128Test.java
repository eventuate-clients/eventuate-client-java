package io.eventuate;

import io.eventuate.Int128;
import org.junit.Assert;
import org.junit.Test;

public class Int128Test {

  @Test
  public void shouldParse() {
    String s = "00000153812efe94-0242ac1100800000";
    Int128 x = Int128.fromString(s);
    Assert.assertEquals(s, x.asString());
  }

  @Test
  public void shouldParse2() {
    Int128 x = new Int128(15, 3);
    String s = "000000000000000f-0000000000000003";
    Assert.assertEquals(s, x.asString());
  }
}
