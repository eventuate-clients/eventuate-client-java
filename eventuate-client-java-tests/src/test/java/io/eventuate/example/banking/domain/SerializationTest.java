package io.eventuate.example.banking.domain;

import io.eventuate.common.json.mapper.JSonMapper;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class SerializationTest {

  @Test
  public void shouldSerdeAccountCreatedEvent() {

    AccountCreatedEvent event = new AccountCreatedEvent(new BigDecimal("123.45"));
    String json = JSonMapper.toJson(event);
    AccountCreatedEvent event2 = JSonMapper.fromJson(json, AccountCreatedEvent.class);

    assertEquals(event, event2);
  }
}
