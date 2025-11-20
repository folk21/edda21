package com.edda21.backend.app.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsulates the logic of selecting questions from the database and attaching them to the given
 * assignment.
 *
 * <p>This implementation: - loads candidate questions from the "question" table - applies optional
 * filters (subject/topic, difficulty) - excludes questions already linked to the target assignment
 * - orders candidates randomly - limits the result by requestedCount - inserts selected questions
 * into "assignment_question" table
 */
@Service
public class QuestionSelectionService {

  private final NamedParameterJdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  public QuestionSelectionService(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  /**
   * Selects questions from the database based on filters and attaches them to the assignment.
   *
   * @param courseId target course id (currently not used in SQL, kept for future extension)
   * @param assignmentId target assignment id
   * @param requestedCount how many questions are requested
   * @param filterJson filters encoded as JSON string (topic/subject, difficulty, etc.)
   * @return number of questions actually selected from DB and linked to the assignment
   */
  @Transactional
  public int selectQuestionsFromDb(
      UUID courseId, UUID assignmentId, int requestedCount, String filterJson) {

    if (requestedCount <= 0) {
      return 0;
    }

    Map<String, Object> filters = parseFilters(filterJson);

    // Build SELECT for candidate question ids
    StringBuilder sql = new StringBuilder();
    sql.append("select q.id ");
    sql.append("from question q ");
    sql.append("where not exists (");
    sql.append("  select 1 from assignment_question aq ");
    sql.append("  where aq.assignment_id = :assignmentId ");
    sql.append("    and aq.question_id = q.id");
    sql.append(") ");

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("assignmentId", assignmentId)
            .addValue("limit", requestedCount);

    // Optional filter: subject/topic
    String subject = extractSubject(filters);
    if (subject != null && !subject.isBlank()) {
      sql.append("and q.subject = :subject ");
      params.addValue("subject", subject);
    }

    // Optional filter: difficulty
    String difficulty = extractDifficulty(filters);
    if (difficulty != null && !difficulty.isBlank()) {
      sql.append("and q.difficulty = :difficulty ");
      params.addValue("difficulty", difficulty);
    }

    // Randomize the order and limit the count
    sql.append("order by random() ");
    sql.append("limit :limit");

    List<UUID> questionIds =
        jdbc.query(sql.toString(), params, (rs, rowNum) -> (UUID) rs.getObject("id"));

    if (questionIds.isEmpty()) {
      return 0;
    }

    // Link selected questions to the assignment
    int count = 0;
    for (UUID qid : questionIds) {
      jdbc.update(
          "insert into assignment_question(assignment_id,question_id,variant,points,ordering) "
              + "values (:a,:q,0,1,0)",
          new MapSqlParameterSource().addValue("a", assignmentId).addValue("q", qid));
      count++;
    }

    return count;
  }

  /** Parses filter JSON into a map. Returns empty map if json is null/blank. */
  private Map<String, Object> parseFilters(String filterJson) {
    if (filterJson == null || filterJson.isBlank()) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(filterJson, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      // In case of invalid JSON we prefer to fail fast rather than silently ignore filters.
      throw new IllegalArgumentException("Invalid filter JSON: " + filterJson, e);
    }
  }

  /**
   * Tries to extract subject from filters. Supports both "subject" and "topic" keys for
   * convenience.
   */
  private String extractSubject(Map<String, Object> filters) {
    Object subject = filters.get("subject");
    if (subject == null) {
      subject = filters.get("topic");
    }
    return subject != null ? String.valueOf(subject) : null;
  }

  /** Tries to extract difficulty from filters. */
  private String extractDifficulty(Map<String, Object> filters) {
    Object difficulty = filters.get("difficulty");
    return difficulty != null ? String.valueOf(difficulty) : null;
  }
}
