package com.edda21.backend.app.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.edda21.testing.AbstractPostgresIntegrationTest;
import com.edda21.testing.assignment.AssignmentTestData;
import com.edda21.testing.db.PostgresTestUtils;
import com.edda21.testing.question.QuestionTestData;
import com.edda21.testing.question.QuestionTestData.QuestionRow;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for {@link QuestionSelectionService} using a real PostgreSQL database.
 *
 * <p>The schema is provided by the db-schema module and created via Flyway migrations.
 *
 * <p>This test suite verifies SQL-based selection logic:
 *
 * <p>- applying filters (subject, difficulty);
 *
 * <p>- enforcing requestedCount limit;
 *
 * <p>- handling no matches;
 *
 * <p>- handling non-positive requestedCount.
 */
class QuestionSelectionServiceTest extends AbstractPostgresIntegrationTest {

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired QuestionSelectionService questionSelectionService;

  @BeforeEach
  void cleanTables() {
    PostgresTestUtils.truncateTables(jdbcTemplate, "assignment_question", "question");
  }

  private void seedFullDataset() {
    QuestionTestData.insertQuestionsFromJson(jdbcTemplate, "db/questions-full.json");
  }

  private String filters(String subject, String difficulty) {
    return
"""
{"subject":"%s","difficulty":"%s"}
"""
        .formatted(subject, difficulty);
  }

  private int select(UUID assignmentId, int requestedCount, String filtersJson) {
    return questionSelectionService.selectQuestionsFromDb(
        UUID.randomUUID(), assignmentId, requestedCount, filtersJson);
  }

  private List<UUID> linkedQuestionIds(UUID assignmentId) {
    return AssignmentTestData.findLinkedQuestionIds(jdbcTemplate, assignmentId);
  }

  private int linkedCount(UUID assignmentId) {
    return AssignmentTestData.countLinks(jdbcTemplate, assignmentId);
  }

  private QuestionRow questionById(UUID id) {
    return QuestionTestData.findById(jdbcTemplate, id);
  }

  /**
   * Verifies that selectQuestionsFromDb applies subject and difficulty filters and links matching
   * questions to the assignment.
   */
  @Test
  void selectQuestionsFromDb_filtersBySubjectAndDifficulty_andLinksToAssignment() {
    seedFullDataset();

    UUID assignmentId = UUID.randomUUID();

    String filtersJson = filters("MATH", "EASY");
    int selected = select(assignmentId, 10, filtersJson);

    assertThat(selected).isGreaterThanOrEqualTo(1);

    List<UUID> linkedIds = linkedQuestionIds(assignmentId);
    assertThat(linkedIds).isNotEmpty();

    assertThat(linkedIds)
        .allSatisfy(
            id -> {
              QuestionRow q = questionById(id);
              assertThat(q.subject()).isEqualTo("MATH");
              assertThat(q.difficulty()).isEqualTo("EASY");
            });
  }

  /**
   * Verifies that selectQuestionsFromDb respects requestedCount when there are more matching
   * questions than requested.
   */
  @Test
  void selectQuestionsFromDb_limitsResultSizeWhenMoreQuestionsThanRequested() {
    seedFullDataset();

    UUID assignmentId = UUID.randomUUID();

    String filtersJson = filters("MATH", "EASY");
    int selected = select(assignmentId, 1, filtersJson);

    assertThat(selected).isEqualTo(1);
    assertThat(linkedCount(assignmentId)).isEqualTo(1);
  }

  /**
   * Verifies handling of edge cases:
   *
   * <p>- no matches for the given filters;
   *
   * <p>- non-positive requestedCount.
   */
  @ParameterizedTest
  @CsvSource({
    // subject, difficulty, requestedCount, expectedSelected
    "GEOGRAPHY, EASY, 5, 0",
    "MATH, EASY, 0, 0"
  })
  void selectQuestionsFromDb_handlesNoMatchesAndNonPositiveRequestedCount(
      String subject, String difficulty, int requestedCount, int expectedSelected) {

    seedFullDataset();

    UUID assignmentId = UUID.randomUUID();
    String filtersJson = filters(subject, difficulty);

    int selected = select(assignmentId, requestedCount, filtersJson);

    assertThat(selected).isEqualTo(expectedSelected);
    assertThat(linkedCount(assignmentId)).isEqualTo(expectedSelected);
  }
}
