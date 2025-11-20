package com.edda21.qp.adapter.in.messaging.kafka;

import java.util.List;
import java.util.UUID;

/**
 * Payload consumed from Kafka by the LLM result processor service.
 *
 * It should be produced by the Python LLM worker and contain:
 * - session id (to update question_generation_session)
 * - assignment id (to link generated questions)
 * - list of generated questions
 * - optional error information, if LLM generation failed
 */
public class LlmQuestionsResponsePayload {

  private UUID sessionId;
  private UUID assignmentId;

  /**
   * List of generated questions. May be empty or null if there was an error.
   */
  private List<LlmQuestionDto> questions;

  /**
   * Non-null error code means that LLM generation failed or returned
   * invalid output. In this case the processor updates the session
   * as FAILED and does not insert any questions.
   */
  private String errorCode;

  private String errorMessage;

  public UUID getSessionId() {
    return sessionId;
  }

  public void setSessionId(UUID sessionId) {
    this.sessionId = sessionId;
  }

  public UUID getAssignmentId() {
    return assignmentId;
  }

  public void setAssignmentId(UUID assignmentId) {
    this.assignmentId = assignmentId;
  }

  public List<LlmQuestionDto> getQuestions() {
    return questions;
  }

  public void setQuestions(List<LlmQuestionDto> questions) {
    this.questions = questions;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
