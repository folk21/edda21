package com.edda21.qp.domain.model;

import java.util.UUID;

/**
 * Simple domain model for question table.
 *
 * <p>This class is intentionally minimal and decoupled from any ORM. It can be used both in the
 * application layer and as a response DTO.
 */
public record Question(
    UUID id, String source, String subject, String difficulty, String body, String correct) {}
