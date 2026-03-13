# ADR 0002: Use Kafka for asynchronous question generation

## Status

Accepted

## Context

LLM-backed question generation can be slower and less predictable than normal CRUD-style request handling.
The instructor-facing API should not block on all downstream generation work.

## Decision

Use Kafka-compatible messaging, implemented locally with Redpanda, for generation requests and related worker processing.

## Consequences

- The backend can acknowledge generation requests quickly.
- Worker logic can evolve independently from the instructor-facing API.
- Retries, idempotency, and status tracking become explicit design topics.
- End-to-end flow observability becomes more important because work no longer happens in a single HTTP request.
