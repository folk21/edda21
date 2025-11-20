package com.edda21.llm.domain.dto;

import com.edda21.llm.domain.model.QuestionDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** A batch of questions, useful for future metadata (e.g., requestId). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionSetDTO(@Valid @NotNull @Size(min = 1) List<QuestionDTO> items) {}
