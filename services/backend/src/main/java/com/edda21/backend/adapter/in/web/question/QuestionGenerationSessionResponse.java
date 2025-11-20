package com.edda21.backend.adapter.in.web.question;

import com.edda21.backend.domain.question.QuestionGenerationSessionStatus;
import com.edda21.backend.domain.question.QuestionSourceMode;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for exposing session status to the instructor UI.
 */
public class QuestionGenerationSessionResponse {

  private UUID sessionId;
  private UUID instructorId;
  private UUID courseId;
  private UUID assignmentId;

  private int requestedCount;
  private int dbSelectedCount;
  private int llmGeneratedCount;

  private QuestionSourceMode mode;
  private QuestionGenerationSessionStatus status;
  private String resultCode;
  private String errorMessage;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  public UUID getSessionId() {
    return sessionId;
  }

  public void setSessionId(UUID sessionId) {
    this.sessionId = sessionId;
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

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
