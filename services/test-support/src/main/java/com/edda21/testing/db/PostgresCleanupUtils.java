package com.edda21.testing.db;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test-only helper for cleaning database state used in question-provider integration tests.
 *
 * <p>The cleanup order respects foreign key constraints between core assessment tables.
 */
public final class PostgresCleanupUtils {

  private PostgresCleanupUtils() {
    // Utility class
  }

  /**
   * Removes data from core assessment tables in the correct order to satisfy foreign keys.
   *
   * <p>This method is intentionally explicit about the table list so that it is easy to see which
   * parts of the schema are affected by a test run.
   */
  public static void cleanDbData(JdbcTemplate jdbcTemplate) {
    // Order matters because of foreign keys.
    jdbcTemplate.update("delete from answer");
    jdbcTemplate.update("delete from assignment_question");
    jdbcTemplate.update("delete from question_generation_session");
    jdbcTemplate.update("delete from assignment");
    jdbcTemplate.update("delete from question");
  }
}
