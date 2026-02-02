package com.edda21.testing.assignment;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test data helpers for the {@code assignment_question} link table.
 *
 * <p>Provides methods for:
 * <p>- linking questions to an assignment;
 * <p>- reading linked question identifiers;
 * <p>- counting links for a given assignment.
 */
public final class AssignmentTestData {

  private AssignmentTestData() {
  }

  /**
   * Representation of a link between an assignment and a question.
   *
   * <p>All fields map directly to columns in the {@code assignment_question} table.
   */
  public record AssignmentQuestionLink(
      UUID assignmentId,
      UUID questionId,
      int variant,
      int points,
      int ordering) {}

  private static final String INSERT_LINK_SQL =
      """
      insert into assignment_question (
        assignment_id,
        question_id,
        variant,
        points,
        ordering
      )
      values (?,?,?,?,?)
      """;

  private static final String SELECT_LINKED_IDS_SQL =
      "select question_id from assignment_question where assignment_id = ?";

  private static final String SELECT_LINKED_COUNT_SQL =
      "select count(*) from assignment_question where assignment_id = ?";

  /**
   * Links the given questions to the assignment using default values for
   * variant, points and ordering.
   *
   * <p>Default values:
   * <p>- variant = 0
   * <p>- points = 1
   * <p>- ordering = 0, 1, 2, ... in the order of the list
   *
   * @param jdbcTemplate JdbcTemplate used to execute SQL
   * @param assignmentId assignment identifier
   * @param questionIds  question identifiers to link
   */
  public static void linkQuestionsWithDefaults(
      JdbcTemplate jdbcTemplate,
      UUID assignmentId,
      List<UUID> questionIds) {

    if (questionIds == null || questionIds.isEmpty()) {
      return;
    }

    jdbcTemplate.batchUpdate(
        INSERT_LINK_SQL,
        questionIds,
        questionIds.size(),
        (ps, questionId) -> {
          int idx = questionIds.indexOf(questionId); // can be replaced with external index
          ps.setObject(1, assignmentId);
          ps.setObject(2, questionId);
          ps.setInt(3, 0);
          ps.setInt(4, 1);
          ps.setInt(5, idx);
        });
  }


  /**
   * Returns all question identifiers linked to the given assignment.
   *
   * @param jdbcTemplate JdbcTemplate used to execute SQL
   * @param assignmentId assignment identifier
   * @return list of question identifiers
   */
  public static List<UUID> findLinkedQuestionIds(
      JdbcTemplate jdbcTemplate,
      UUID assignmentId) {

    return jdbcTemplate.query(
        SELECT_LINKED_IDS_SQL,
        (rs, rowNum) -> (UUID) rs.getObject(1),
        assignmentId);
  }

  /**
   * Returns the number of question links for the given assignment.
   *
   * @param jdbcTemplate JdbcTemplate used to execute SQL
   * @param assignmentId assignment identifier
   * @return number of rows in {@code assignment_question} for this assignment
   */
  public static int countLinks(JdbcTemplate jdbcTemplate, UUID assignmentId) {
    Integer count =
        jdbcTemplate.queryForObject(
            SELECT_LINKED_COUNT_SQL,
            Integer.class,
            assignmentId);
    return count == null ? 0 : count;
  }
}
