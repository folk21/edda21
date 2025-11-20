package com.edda21.qp.domain.model;

import java.util.List;

/**
 * View model that aggregates assignment and its questions.
 *
 * <p>This is what we will return from the read endpoint.
 */
public record AssignmentWithQuestions(Assignment assignment, List<Question> questions) {}
