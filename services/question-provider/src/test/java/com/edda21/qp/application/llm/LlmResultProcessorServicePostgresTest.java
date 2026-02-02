package com.edda21.qp.application.llm;

import static com.edda21.testing.db.PostgresCleanupUtils.cleanDbData;
import static org.assertj.core.api.Assertions.assertThat;

import com.edda21.qp.adapter.in.messaging.kafka.LlmQuestionDto;
import com.edda21.qp.adapter.in.messaging.kafka.LlmQuestionsResponsePayload;
import com.edda21.testing.AbstractPostgresIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for {@link LlmResultProcessorService} against a real PostgreSQL schema using
 * Flyway migrations.
 *
 * <p>Key ideas:
 *
 * <ul>
 *   <li>We reuse {@link AbstractPostgresIntegrationTest} so that a single PostgreSQL Testcontainers
 *       instance is started and Flyway migrations are applied.
 *   <li>Tests treat {@link LlmResultProcessorService} as a black box: they insert an initial {@code
 *       question_generation_session} row, call {@link
 *       LlmResultProcessorService#processResponse(LlmQuestionsResponsePayload)} and assert the
 *       resulting database state.
 *   <li>We explicitly clean and adjust the schema where necessary (e.g. add columns used only by
 *       LLM integration) to keep the tests stable even when migrations evolve.
 * </ul>
 */
class LlmResultProcessorServicePostgresTest extends AbstractPostgresIntegrationTest {

  @Autowired LlmResultProcessorService processorService;

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanDatabase() {
    // clean DB data
    cleanDbData(jdbcTemplate);

    // Make sure the columns used by LlmResultProcessorService exist.
    // In production this should be covered by a dedicated Flyway migration,
    // but in tests we keep it defensive.
    jdbcTemplate.execute("alter table question add column if not exists text text");
    jdbcTemplate.execute(
        "alter table question add column if not exists created_at timestamptz default now()");
    jdbcTemplate.execute(
        "alter table question add column if not exists updated_at timestamptz default now()");
  }

  @Test
  void processResponse_withError_updatesSessionAndDoesNotInsertQuestions() {
    UUID sessionId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();

    insertSession(
        sessionId,
        assignmentId,
        /*instructorId*/ UUID.randomUUID(),
        /*courseId*/ UUID.randomUUID(),
        /*requestedCount*/ 5,
        /*dbSelectedCount*/ 2,
        /*status*/ "PENDING");

    LlmQuestionsResponsePayload payload = new LlmQuestionsResponsePayload();
    payload.setSessionId(sessionId);
    payload.setAssignmentId(assignmentId);
    payload.setErrorCode("LLM_TIMEOUT");
    payload.setErrorMessage("LLM call timed out");

    processorService.processResponse(payload);

    int questionCount = jdbcTemplate.queryForObject("select count(*) from question", Integer.class);
    int linkCount =
        jdbcTemplate.queryForObject("select count(*) from assignment_question", Integer.class);

    SessionStatusRow row =
        jdbcTemplate.queryForObject(
            """
            select status, result_code, error_message
            from question_generation_session
            where id = ?
            """,
            (rs, rowNum) ->
                new SessionStatusRow(
                    rs.getString("status"),
                    rs.getString("result_code"),
                    rs.getString("error_message")),
            sessionId);

    // No questions must be inserted and no links must be created.
    assertThat(questionCount).isZero();
    assertThat(linkCount).isZero();

    // Session must be marked as FAILED with the provided error code and message.
    assertThat(row.status()).isEqualTo("FAILED");
    assertThat(row.resultCode()).isEqualTo("LLM_TIMEOUT");
    assertThat(row.errorMessage()).isEqualTo("LLM call timed out");
  }

  @Test
  void processResponse_withValidQuestions_insertsQuestionsLinksAndUpdatesSession() {
    UUID sessionId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();

    // requested_count = 5, db_selected_count = 2
    // we will generate 3 questions but one of them will be skipped (blank text),
    // so effective llm_generated_count = 2 and total = 4 < 5 => OK_PARTIAL_DB_AND_LLM.
    insertSession(
        sessionId,
        assignmentId,
        /*instructorId*/ UUID.randomUUID(),
        /*courseId*/ UUID.randomUUID(),
        /*requestedCount*/ 5,
        /*dbSelectedCount*/ 2,
        /*status*/ "PENDING");

    LlmQuestionDto q1 = new LlmQuestionDto();
    q1.setText("What is 2 + 2?");
    q1.setSubject("MATH");
    q1.setDifficulty("EASY");

    LlmQuestionDto q2 = new LlmQuestionDto();
    // This question will be skipped because text is blank.
    q2.setText("   ");
    q2.setSubject("MATH");
    q2.setDifficulty("EASY");

    LlmQuestionDto q3 = new LlmQuestionDto();
    q3.setText("What is the capital of France?");
    q3.setSubject("GEOGRAPHY");
    q3.setDifficulty("MEDIUM");

    LlmQuestionsResponsePayload payload = new LlmQuestionsResponsePayload();
    payload.setSessionId(sessionId);
    payload.setAssignmentId(assignmentId);
    payload.setQuestions(List.of(q1, q2, q3));

    processorService.processResponse(payload);

    int questionCount = jdbcTemplate.queryForObject("select count(*) from question", Integer.class);
    int linkCountForAssignment =
        jdbcTemplate.queryForObject(
            "select count(*) from assignment_question where assignment_id = ?",
            Integer.class,
            assignmentId);

    SessionCountersRow counters =
        jdbcTemplate.queryForObject(
            """
            select requested_count, db_selected_count, llm_generated_count, status, result_code
            from question_generation_session
            where id = ?
            """,
            (rs, rowNum) ->
                new SessionCountersRow(
                    rs.getInt("requested_count"),
                    rs.getInt("db_selected_count"),
                    rs.getInt("llm_generated_count"),
                    rs.getString("status"),
                    rs.getString("result_code")),
            sessionId);

    // Only 2 valid questions should be inserted and linked to the assignment.
    assertThat(questionCount).isEqualTo(2);
    assertThat(linkCountForAssignment).isEqualTo(2);

    // Session counters and status/result code must reflect the LLM output.
    assertThat(counters.requestedCount()).isEqualTo(5);
    assertThat(counters.dbSelectedCount()).isEqualTo(2);
    assertThat(counters.llmGeneratedCount()).isEqualTo(2);
    assertThat(counters.status()).isEqualTo("COMPLETED");
    assertThat(counters.resultCode()).isEqualTo("OK_PARTIAL_DB_AND_LLM");
  }

  private void insertSession(
      UUID sessionId,
      UUID assignmentId,
      UUID instructorId,
      UUID courseId,
      int requestedCount,
      int dbSelectedCount,
      String status) {

    jdbcTemplate.update(
        """
        insert into question_generation_session (
          id,
          instructor_id,
          course_id,
          assignment_id,
          requested_count,
          db_selected_count,
          mode,
          status
        ) values (?,?,?,?,?,?,?,?)
        """,
        sessionId,
        instructorId,
        courseId,
        assignmentId,
        requestedCount,
        dbSelectedCount,
        "DB_THEN_LLM",
        status);
  }

  /** Minimal projection of session status for assertions. */
  private record SessionStatusRow(String status, String resultCode, String errorMessage) {}

  /** Minimal projection of session counters for assertions. */
  private record SessionCountersRow(
      int requestedCount,
      int dbSelectedCount,
      int llmGeneratedCount,
      String status,
      String resultCode) {}
}
