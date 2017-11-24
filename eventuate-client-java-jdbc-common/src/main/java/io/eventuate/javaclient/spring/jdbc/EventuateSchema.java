package io.eventuate.javaclient.spring.jdbc;

import org.apache.commons.lang.StringUtils;

import java.util.Objects;

public class EventuateSchema {
  public static final String DEFAULT_SCHEMA = "eventuate";
  public static final String EMPTY_SCHEMA = "none";

  private final String eventuateDatabaseSchema;

  public EventuateSchema() {
    eventuateDatabaseSchema = DEFAULT_SCHEMA;
  }

  public EventuateSchema(String eventuateDatabaseSchema) {
    this.eventuateDatabaseSchema = StringUtils.isBlank(eventuateDatabaseSchema) ? DEFAULT_SCHEMA : eventuateDatabaseSchema;
  }

  public String getEventuateDatabaseSchema() {
    return eventuateDatabaseSchema;
  }

  public boolean isEmpty() {
    return EMPTY_SCHEMA.equals(eventuateDatabaseSchema);
  }

  public boolean isDefault() {
    return DEFAULT_SCHEMA.equals(eventuateDatabaseSchema);
  }

  public String qualifyTable(String table) {
    if (isEmpty()) return table;

    String schema = isDefault() ? DEFAULT_SCHEMA : eventuateDatabaseSchema;

    return String.format("%s.%s", schema, table);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EventuateSchema that = (EventuateSchema) o;

    return Objects.equals(that.eventuateDatabaseSchema, eventuateDatabaseSchema);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(eventuateDatabaseSchema);
  }
}
