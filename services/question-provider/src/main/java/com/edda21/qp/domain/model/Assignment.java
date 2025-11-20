package com.edda21.qp.domain.model;

import java.util.UUID;

/**
 * Minimal domain model for assignment table.
 *
 * <p>It represents a single assignment row. Questions are linked via the assignment_question table.
 */
public record Assignment(UUID id, UUID courseId, String title) {}
