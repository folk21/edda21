package com.edda21.qp.domain.model;

/** Defines how questions should be obtained for a session. */
public enum QuestionSourceMode {
  /** Only database questions are used. */
  DB_ONLY,

  /** First try to load questions from the database, then ask LLM to generate missing ones. */
  DB_THEN_LLM,

  /** Only LLM-generated questions are used. */
  LLM_ONLY
}
