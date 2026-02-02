package com.edda21.testing.question;

import com.edda21.testing.json.TestJsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test data helpers for the {@code question} table.
 *
 * <p>Provides convenient methods for:
 * <p>- loading question rows from JSON test resources;
 * <p>- inserting question rows into the database;
 * <p>- reading a single question row by its identifier.
 */
public final class QuestionTestData {

  private QuestionTestData() {
  }

  /**
   * Minimal representation of a question row used in tests.
   *
   * <p>This matches the subset of columns used in selection tests:
   * <p>- id
   * <p>- subject
   * <p>- difficulty
   */
  public record QuestionRow(UUID id, String subject, String difficulty) {}

  private static final String INSERT_QUESTION_SQL =
      "insert into question (id, subject, difficulty) values (?,?,?)";

  private static final String SELECT_QUESTION_ROW_SQL =
      "select id, subject, difficulty from question where id = ?";

  /**
   * Loads questions from a JSON file located on the classpath.
   *
   * <p>The JSON file is expected to contain an array of objects with fields
   * compatible with {@link QuestionRow}.
   *
   * @param classpathLocation resource location, e.g. {@code "db/questions-full.json"}
   * @return list of deserialized question rows
   */
  public static List<QuestionRow> loadFromJson(String classpathLocation) {
    return TestJsonUtils.readFromClasspath(
        classpathLocation,
        new TypeReference<List<QuestionRow>>() {});
  }

  /**
   * Inserts question rows into the database using a batch insert.
   *
   * @param jdbcTemplate JdbcTemplate used to execute SQL
   * @param rows         question rows to insert
   */
  public static void insertQuestions(JdbcTemplate jdbcTemplate, List<QuestionRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    jdbcTemplate.batchUpdate(
        INSERT_QUESTION_SQL,
        rows,
        rows.size(),
        (ps, row) -> {
          ps.setObject(1, row.id());
          ps.setString(2, row.subject());
          ps.setString(3, row.difficulty());
        });
  }

  /**
   * Convenience method that loads questions from a JSON resource and inserts them
   * into the database in a single call.
   *
   * @param jdbcTemplate      JdbcTemplate used to execute SQL
   * @param classpathLocation resource location, e.g. {@code "db/questions-full.json"}
   */
  public static void insertQuestionsFromJson(
      JdbcTemplate jdbcTemplate,
      String classpathLocation) {

    List<QuestionRow> rows = loadFromJson(classpathLocation);
    insertQuestions(jdbcTemplate, rows);
  }

  /**
   * Reads a single question row by identifier.
   *
   * <p>Throws an exception if the row is not found.
   *
   * @param jdbcTemplate JdbcTemplate used to execute SQL
   * @param id           question identifier
   * @return question row
   */
  public static QuestionRow findById(JdbcTemplate jdbcTemplate, UUID id) {
    return jdbcTemplate.queryForObject(
        SELECT_QUESTION_ROW_SQL,
        (rs, rowNum) ->
            new QuestionRow(
                (UUID) rs.getObject("id"),
                rs.getString("subject"),
                rs.getString("difficulty")),
        id);
  }
}
