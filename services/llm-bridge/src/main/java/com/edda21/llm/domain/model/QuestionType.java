package com.edda21.llm.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type-safe question kind used across the EdTech platform.
 *
 * <p>Design notes: - We accept case-insensitive JSON strings ("open", "OPEN", "MULTI_choice",
 * "multi_choice", ...). - @JsonCreator makes Jackson convert incoming JSON -> enum. - @JsonValue
 * makes Jackson serialize enum -> JSON string (e.g., "OPEN").
 */
public enum QuestionType {

  /**
   * OPEN — Free-form answer (short text).
   *
   * <p>Semantics: - The student types a short textual answer. - No "options" array is
   * expected/present. - Auto-grading is limited (string/keyword match) unless enhanced by LLM.
   *
   * <p>UI expectations: - Render a text input field (single- or multi-line).
   *
   * <p>Validation hints: - "body" must be non-blank. - "correct" may be a keyword/short phrase (or
   * left for manual/LLM review).
   */
  OPEN,

  /**
   * MULTI_CHOICE — Multiple-Choice Question (single correct option in our current design).
   *
   * <p>Semantics: - The student selects ONE option from a finite list. - "options" MUST be present
   * and contain the "correct" value. - Options should be distinct, human-readable strings
   * (typically 3–6 items).
   *
   * <p>UI expectations: - Render as a radio-button list (single select).
   *
   * <p>Validation hints: - "options" is required and non-empty; must include "correct". - "body"
   * must be non-blank; "correct" non-blank and present in options.
   */
  MULTI_CHOICE;

  public static QuestionType DEFAULT = OPEN;

  /**
   * Case-insensitive parsing from JSON. Throws IllegalArgumentException on unknown values. This
   * fails fast and surfaces bad payloads early.
   */
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static QuestionType fromJson(String raw) {
    if (raw == null) throw new IllegalArgumentException("type is null");
    return QuestionType.valueOf(raw.trim().toUpperCase());
  }

  public static QuestionType orDefault(QuestionType type) {
    return type == null ? DEFAULT : type;
  }

  /** Serialize enum to a stable JSON string value ("OPEN" / "MULTI_CHOICE"). */
  @JsonValue
  public String toJson() {
    return name();
  }
}
