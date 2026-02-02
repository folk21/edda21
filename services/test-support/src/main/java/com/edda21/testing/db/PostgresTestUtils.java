package com.edda21.testing.db;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Utility methods for working with PostgreSQL in integration tests.
 *
 * <p>Provides helpers for truncating tables, deleting data and querying simple
 * statistics such as row counts.
 */
public final class PostgresTestUtils {

  private PostgresTestUtils() {
  }

  /**
   * Truncates the given tables in a single statement.
   *
   * <p>Using a single TRUNCATE statement for all tables helps with foreign key
   * constraints, because PostgreSQL can validate all relationships at once.
   *
   * @param jdbcTemplate JdbcTemplate to execute SQL against
   * @param tableNames   names of tables to truncate
   */
  public static void truncateTables(JdbcTemplate jdbcTemplate, String... tableNames) {
    String joined = joinTableNames(tableNames);
    if (joined.isEmpty()) {
      return;
    }
    jdbcTemplate.execute("truncate table " + joined);
  }

  /**
   * Truncates the given tables with CASCADE.
   *
   * <p>This is useful for complex schemas with deep foreign key relationships
   * where a plain TRUNCATE is not sufficient.
   *
   * @param jdbcTemplate JdbcTemplate to execute SQL against
   * @param tableNames   names of tables to truncate
   */
  public static void truncateTablesCascade(JdbcTemplate jdbcTemplate, String... tableNames) {
    String joined = joinTableNames(tableNames);
    if (joined.isEmpty()) {
      return;
    }
    jdbcTemplate.execute("truncate table " + joined + " cascade");
  }

  /**
   * Deletes all rows from the given tables.
   *
   * <p>This is a safer but potentially slower alternative to TRUNCATE. It may be
   * useful when TRUNCATE is not allowed or when you want to preserve sequences.
   *
   * @param jdbcTemplate JdbcTemplate to execute SQL against
   * @param tableNames   names of tables to delete from
   */
  public static void deleteFromTables(JdbcTemplate jdbcTemplate, String... tableNames) {
    if (tableNames == null || tableNames.length == 0) {
      return;
    }
    for (String table : tableNames) {
      String trimmed = table == null ? "" : table.trim();
      if (!trimmed.isEmpty()) {
        jdbcTemplate.execute("delete from " + trimmed);
      }
    }
  }

  /**
   * Returns the number of rows in the given table.
   *
   * @param jdbcTemplate JdbcTemplate to execute SQL against
   * @param tableName    table name
   * @return number of rows in the table
   */
  public static int countRows(JdbcTemplate jdbcTemplate, String tableName) {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from " + tableName, Integer.class);
    return count == null ? 0 : count;
  }

  /**
   * Returns whether the given table contains at least one row.
   *
   * @param jdbcTemplate JdbcTemplate to execute SQL against
   * @param tableName    table name
   * @return true if the table contains at least one row
   */
  public static boolean hasAnyRows(JdbcTemplate jdbcTemplate, String tableName) {
    return countRows(jdbcTemplate, tableName) > 0;
  }

  private static String joinTableNames(String... tableNames) {
    if (tableNames == null || tableNames.length == 0) {
      return "";
    }
    List<String> cleaned =
        Arrays.stream(tableNames)
            .map(name -> name == null ? "" : name.trim())
            .filter(name -> !name.isEmpty())
            .toList();
    return cleaned.stream().collect(Collectors.joining(", "));
  }
}
