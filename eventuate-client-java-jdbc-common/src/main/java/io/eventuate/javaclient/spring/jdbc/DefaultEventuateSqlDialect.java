package io.eventuate.javaclient.spring.jdbc;

public class DefaultEventuateSqlDialect implements EventuateSqlDialect{

  @Override
  public boolean supports(String driver) {
    return true;
  }

  @Override
  public String addLimitToSql(String sql, String limitExpression) {
    return String.format("%s limit %s", sql, limitExpression);
  }
}
