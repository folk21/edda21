package com.edda21.backend.domain.question;

/**
 * Status of a question generation session lifecycle.
 */
public enum QuestionGenerationSessionStatus {

  /**
   * Session has been created but processing has not started yet.
   */
  PENDING,

  /**
   * Session is currently being processed (DB and/or LLM).
   */
  IN_PROGRESS,

  /**
   * Session processing has finished (possibly with a partial result).
   */
  COMPLETED,

  /**
   * Session processing failed due to an unrecoverable error.
   */
  FAILED
}
