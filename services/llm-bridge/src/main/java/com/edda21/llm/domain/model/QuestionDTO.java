package com.edda21.llm.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Strongly typed, immutable question DTO. Optional fields (topic, options, explanation) may be
 * null. For MULTI_CHOICE, 'options' should be provided by the generator.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionDTO(
    @NotBlank String source,
    @NotBlank String subject,
    String topic,
    @NotBlank String difficulty, // e.g., A1..C2 or B1
    @NotBlank QuestionType type, // OPEN or MULTI_CHOICE
    @NotBlank String body,
    List<String> options, // required if type == MULTI_CHOICE; otherwise null
    @NotBlank String correct,
    String explanation) {}
