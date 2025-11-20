package com.edda21.qp.application.llm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edda21.qp.adapter.in.messaging.kafka.LlmQuestionsResponsePayload;
import com.edda21.qp.adapter.in.messaging.kafka.LlmQuestionDto;

/**
 * Processes LLM responses:
 * - inserts generated questions into the "question" table
 * - links them to the assignment via "assignment_question"
 * - updates "question_generation_session" with LLM counters and result codes
 */
@Service
public class LlmResultProcessorService {

  private final NamedParameterJdbcTemplate jdbc;

  public LlmResultProcessorService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Main entry point for handling a single LLM response.
   * This method is intended to be called by the Kafka listener.
   */
  @Transactional
  public void processResponse(LlmQuestionsResponsePayload payload) {
    UUID sessionId = payload.getSessionId();
    UUID assignmentId = payload.getAssignmentId();

    if (sessionId == null || assignmentId == null) {
      throw new IllegalArgumentException(
          "LLM response payload must contain both sessionId and assignmentId");
    }

    if (payload.getErrorCode() != null) {
      updateSessionOnError(sessionId, payload.getErrorCode(), payload.getErrorMessage());
      return;
    }

    int createdCount = insertQuestionsAndLinkToAssignment(assignmentId, payload.getQuestions());

    updateSessionOnSuccess(sessionId, createdCount);
  }

  /**
   * Inserts generated questions into "question" table and links them
   * to the target assignment via "assignment_question".
   *
   * IMPORTANT: You may need to adjust column names and default values
   * here according to your real schema of the "question" table.
   *
   * @return number of successfully created questions
   */
  private int insertQuestionsAndLinkToAssignment(
      UUID assignmentId,
      List<LlmQuestionDto> questions
  ) {
    if (questions == null || questions.isEmpty()) {
      return 0;
    }

    int ordering = getCurrentMaxOrdering(assignmentId);

    int created = 0;
    for (LlmQuestionDto dto : questions) {
      if (dto.getText() == null || dto.getText().isBlank()) {
        // Skip invalid question without text
        continue;
      }

      UUID questionId = UUID.randomUUID();

      // Insert into "question" table.
      // Adjust columns to match your actual schema (subject/difficulty/metadata).
      MapSqlParameterSource questionParams = new MapSqlParameterSource()
          .addValue("id", questionId)
          .addValue("text", dto.getText())
          .addValue("subject", dto.getSubject())
          .addValue("difficulty", dto.getDifficulty())
          .addValue("source", "LLM")
          .addValue("created_at", OffsetDateTime.now())
          .addValue("updated_at", OffsetDateTime.now());

      jdbc.update(
          """
              insert into question(id, text, subject, difficulty, source, created_at, updated_at)
              values (:id, :text, :subject, :difficulty, :source, :created_at, :updated_at)
              """,
          questionParams
      );

      // Link question to assignment.
      MapSqlParameterSource linkParams = new MapSqlParameterSource()
          .addValue("assignmentId", assignmentId)
          .addValue("questionId", questionId)
          .addValue("variant", 0)
          .addValue("points", 1)
          .addValue("ordering", ++ordering);

      jdbc.update(
          """
              insert into assignment_question(assignment_id, question_id, variant, points, ordering)
              values (:assignmentId, :questionId, :variant, :points, :ordering)
              """,
          linkParams
      );

      created++;
    }

    return created;
  }

  /**
   * Returns the current maximum "ordering" value for questions linked
   * to the given assignment. Used to append new questions in sequence.
   */
  private int getCurrentMaxOrdering(UUID assignmentId) {
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("assignmentId", assignmentId);

    Integer value = jdbc.queryForObject(
        "select coalesce(max(ordering), 0) as max_ordering "
            + "from assignment_question "
            + "where assignment_id = :assignmentId",
        params,
        Integer.class
    );

    return value != null ? value : 0;
  }

  /**
   * Updates question_generation_session row when LLM processing failed.
   */
  private void updateSessionOnError(UUID sessionId, String errorCode, String errorMessage) {
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", sessionId)
        .addValue("status", "FAILED")
        .addValue("resultCode", errorCode != null ? errorCode : "LLM_ERROR")
        .addValue("errorMessage", errorMessage)
        .addValue("updatedAt", OffsetDateTime.now());

    int updated = jdbc.update(
        """
            update question_generation_session
            set status = :status,
                result_code = :resultCode,
                error_message = :errorMessage,
                updated_at = :updatedAt
            where id = :id
            """,
        params
    );

    if (updated == 0) {
      throw new IllegalStateException(
          "Failed to update question_generation_session for error, session not found: " + sessionId);
    }
  }

  /**
   * Updates question_generation_session row when LLM processing succeeded.
   * Session is marked as COMPLETED and resultCode is set based on
   * requested_count / db_selected_count / llm_generated_count.
   */
  private void updateSessionOnSuccess(UUID sessionId, int llmGeneratedCount) {
    SessionRow sessionRow = loadSessionForUpdate(sessionId);

    int requestedCount = sessionRow.requestedCount;
    int dbSelectedCount = sessionRow.dbSelectedCount;

    int total = dbSelectedCount + llmGeneratedCount;

    String resultCode;
    if (llmGeneratedCount == 0) {
      resultCode = "LLM_EMPTY_RESULT";
    } else if (total >= requestedCount) {
      resultCode = "OK_FULL";
    } else {
      resultCode = "OK_PARTIAL_DB_AND_LLM";
    }

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", sessionId)
        .addValue("llmGeneratedCount", llmGeneratedCount)
        .addValue("status", "COMPLETED")
        .addValue("resultCode", resultCode)
        .addValue("updatedAt", OffsetDateTime.now());

    int updated = jdbc.update(
        """
            update question_generation_session
            set llm_generated_count = :llmGeneratedCount,
                status = :status,
                result_code = :resultCode,
                updated_at = :updatedAt
            where id = :id
            """,
        params
    );

    if (updated == 0) {
      throw new IllegalStateException(
          "Failed to update question_generation_session for success, session not found: " + sessionId);
    }
  }

  /**
   * Loads session row inside the current transaction so that
   * requested_count and db_selected_count are consistent.
   */
  private SessionRow loadSessionForUpdate(UUID sessionId) {
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", sessionId);

    return jdbc.queryForObject(
        """
            select requested_count, db_selected_count
            from question_generation_session
            where id = :id
            for update
            """,
        params,
        new SessionRowMapper()
    );
  }

  /**
   * Internal DTO for session row.
   */
  private record SessionRow(int requestedCount, int dbSelectedCount) {}

  private static class SessionRowMapper implements RowMapper<SessionRow> {
    @Override
    public SessionRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      int requested = rs.getInt("requested_count");
      int dbSelected = rs.getInt("db_selected_count");
      return new SessionRow(requested, dbSelected);
    }
  }
}
