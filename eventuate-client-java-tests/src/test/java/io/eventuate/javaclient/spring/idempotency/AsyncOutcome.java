package io.eventuate.javaclient.spring.idempotency;

import io.eventuate.EntityWithIdAndVersion;
import io.eventuate.example.banking.domain.Account;
import org.apache.commons.lang.builder.ToStringBuilder;

public class AsyncOutcome {
  public final EntityWithIdAndVersion<Account> before;
  public final EntityWithIdAndVersion<Account> after;

  public AsyncOutcome(EntityWithIdAndVersion<Account> before, EntityWithIdAndVersion<Account> after) {
    this.before = before;
    this.after = after;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
