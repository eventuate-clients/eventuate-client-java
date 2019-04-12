package io.eventuate.javaclient.spring.jdbc;

public interface EventuateSqlDialect {
  boolean supports(String driver);
  String addLimitToSql(String sql, String limitExpression);
}
