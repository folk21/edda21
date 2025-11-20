package com.edda21.backend.domain.question;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a single request to prepare a batch of questions
 * for a specific instructor / course / assignment.
 *
 * This entity is mapped to the "question_generation_session" table.
 * You need Spring Data JPA + PostgreSQL driver for it to work.
 */
@Entity
@Table(name = "question_generation_session")
public class QuestionGenerationSession {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "instructor_id", nullable = false)
  private UUID instructorId;

  @Column(name = "course_id")
  private UUID courseId;

  @Column(name = "assignment_id")
  private UUID assignmentId;

  @Column(name = "requested_count", nullable = false)
  private int requestedCount;

  @Column(name = "db_selected_count", nullable = false)
  private int dbSelectedCount;

  @Column(name = "llm_generated_count", nullable = false)
  private int llmGeneratedCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "mode", nullable = false, length = 32)
  private QuestionSourceMode mode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private QuestionGenerationSessionStatus status;

  @Column(name = "result_code", length = 64)
  private String resultCode;

  @Column(name = "error_message")
  private String errorMessage;

  /**
   * Optional filter parameters as raw JSON.
   * Stored as TEXT, but can be switched to JSON/JSONB on DB side.
   */
  @Column(name = "filter_json")
  private String filterJson;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getInstructorId() {
    return instructorId;
  }

  public void setInstructorId(UUID instructorId) {
    this.instructorId = instructorId;
  }

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

  public int getDbSelectedCount() {
    return dbSelectedCount;
  }

  public void setDbSelectedCount(int dbSelectedCount) {
    this.dbSelectedCount = dbSelectedCount;
  }

  public int getLlmGeneratedCount() {
    return llmGeneratedCount;
  }

  public void setLlmGeneratedCount(int llmGeneratedCount) {
    this.llmGeneratedCount = llmGeneratedCount;
  }

  public QuestionSourceMode getMode() {
    return mode;
  }

  public void setMode(QuestionSourceMode mode) {
    this.mode = mode;
  }

  public QuestionGenerationSessionStatus getStatus() {
    return status;
  }

  public void setStatus(QuestionGenerationSessionStatus status) {
    this.status = status;
  }

  public String getResultCode() {
    return resultCode;
  }

  public void setResultCode(String resultCode) {
    this.resultCode = resultCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getFilterJson() {
    return filterJson;
  }

  public void setFilterJson(String filterJson) {
    this.filterJson = filterJson;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
