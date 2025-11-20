package com.edda21.backend.adapter.in.web.question;

import com.edda21.qp.domain.model.QuestionSourceMode;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a new question generation session.
 */
public class QuestionGenerationSessionCreateRequest {

  private UUID courseId;
  private UUID assignmentId;

  private int requestedCount;

  private QuestionSourceMode mode;

  /**
   * Filter parameters (topic, difficulty, tags, etc.).
   */
  private Map<String, Object> filters;

  public UUID getCourseId() {
    return courseId;
  }

  public void setCourseId(UUID courseId) {
    this.courseId = courseId;
  }

  public UUID getAssignmentId() {
    return assignmentId;
  }

  public void setAssignmentId(UUID assignmentId) {
    this.assignmentId = assignmentId;
  }

  public int getRequestedCount() {
    return requestedCount;
  }

  public void setRequestedCount(int requestedCount) {
    this.requestedCount = requestedCount;
  }

  public QuestionSourceMode getMode() {
    return mode;
  }

  public void setMode(QuestionSourceMode mode) {
    this.mode = mode;
  }

  public Map<String, Object> getFilters() {
    return filters;
  }

  public void setFilters(Map<String, Object> filters) {
    this.filters = filters;
  }
}
